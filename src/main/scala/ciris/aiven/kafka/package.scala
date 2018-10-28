package ciris.aiven

import java.security.KeyStore
import java.security.KeyStore.{PasswordProtection, PrivateKeyEntry}

import better.files._
import ciris._
import ciris.api._
import ciris.api.syntax._

package object kafka {
  implicit val aivenKafkaClientPrivateKeyConfigDecoder: ConfigDecoder[String, AivenKafkaClientPrivateKey] =
    ConfigDecoder.fromTry("AivenKafkaClientPrivateKey")(AivenKafkaClientPrivateKey.fromString).redactSensitive

  implicit val aivenKafkaClientCertificateConfigDecoder: ConfigDecoder[String, AivenKafkaClientCertificate] =
    ConfigDecoder.fromTry("AivenKafkaClientCertificate")(AivenKafkaClientCertificate.fromString).redactSensitive

  implicit val aivenKafkaServiceCertificateConfigDecoder: ConfigDecoder[String, AivenKafkaServiceCertificate] =
    ConfigDecoder.fromTry("AivenKafkaServiceCertificate")(AivenKafkaServiceCertificate.fromString).redactSensitive

  def aivenKafkaSetup[F[_]](
    clientPrivateKey: ConfigValue[F, AivenKafkaClientPrivateKey],
    clientCertificate: ConfigValue[F, AivenKafkaClientCertificate],
    serviceCertificate: ConfigValue[F, AivenKafkaServiceCertificate]
  )(implicit F: Sync[F]): ConfigValue[F, AivenKafkaSetupDetails] = ConfigValue.applyF {
    (clientPrivateKey.value product clientCertificate.value product serviceCertificate.value)
      .flatMap {
        case ((Right(clientPrivateKey), Right(clientCertificate)), Right(serviceCertificate)) =>
          setupKeyAndTrustStores(clientPrivateKey, clientCertificate, serviceCertificate)
        case ((clientPrivateKey, clientCertificate), serviceCertificate) =>
          F.pure {
            ConfigError.left {
              List(clientPrivateKey, clientCertificate, serviceCertificate)
                .collect { case Left(error) => error }
                .reduce(_ combine _)
            }
          }
      }
  }

  private def setupKeyAndTrustStores[F[_]](
    clientPrivateKey: AivenKafkaClientPrivateKey,
    clientCertificate: AivenKafkaClientCertificate,
    serviceCertificate: AivenKafkaServiceCertificate
  )(implicit F: Sync[F]): F[Either[ConfigError, AivenKafkaSetupDetails]] =
    F.handleErrorWith {
      for {
        setupDetails <- AivenKafkaSetupDetails.newTemporary[F]
        _ <- setupKeyStore(
          clientPrivateKey = clientPrivateKey,
          clientCertificate = clientCertificate,
          keyStoreFile = setupDetails.keyStoreFile,
          keyStorePassword = setupDetails.keyStorePassword
        )
        _ <- setupTrustStore(
          serviceCertificate = serviceCertificate,
          trustStoreFile = setupDetails.trustStoreFile,
          trustStorePassword = setupDetails.trustStorePassword
        )
      } yield ConfigError.right(setupDetails)
    } { throwable =>
      F.pure {
        ConfigError.left {
          ConfigError.sensitive(
            message = s"Failed to setup Aiven Kafka key and trust stores: $throwable",
            redactedMessage = "Failed to setup Aiven Kafka key and trust stores"
          )
        }
      }
    }

  private def setupStore[F[_]](
    storeType: String,
    storeFile: File,
    storePasswordChars: Array[Char]
  )(setupStore: KeyStore => Unit)(
    implicit F: Sync[F]
  ): F[Unit] =
    F.suspend {
      F.pure {
        val keyStore = KeyStore.getInstance(storeType)
        keyStore.load(null, storePasswordChars)
        setupStore(keyStore)

        for {
          outputStream <- storeFile.newOutputStream.autoClosed
        } keyStore.store(outputStream, storePasswordChars)
      }
    }

  private def setupKeyStore[F[_]](
    clientPrivateKey: AivenKafkaClientPrivateKey,
    clientCertificate: AivenKafkaClientCertificate,
    keyStoreFile: AivenKafkaKeyStoreFile,
    keyStorePassword: AivenKafkaKeyStorePassword
  )(implicit F: Sync[F]): F[Unit] = {
    val keyStorePasswordChars =
      keyStorePassword.value.toCharArray

    setupStore[F](
      storeType = "PKCS12",
      storeFile = keyStoreFile.value,
      storePasswordChars = keyStorePasswordChars
    ) { store =>
      store.setEntry(
        "service_key",
        new PrivateKeyEntry(
          clientPrivateKey.value,
          Array(clientCertificate.value)
        ),
        new PasswordProtection(keyStorePasswordChars)
      )
    }
  }

  private def setupTrustStore[F[_]](
    serviceCertificate: AivenKafkaServiceCertificate,
    trustStoreFile: AivenKafkaTrustStoreFile,
    trustStorePassword: AivenKafkaTrustStorePassword
  )(implicit F: Sync[F]): F[Unit] = {
    setupStore[F](
      storeType = "JKS",
      storeFile = trustStoreFile.value,
      storePasswordChars = trustStorePassword.value.toCharArray
    ) { _.setCertificateEntry("CA", serviceCertificate.value) }
  }
}
