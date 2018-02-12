package ciris.aiven

import java.security.KeyStore
import java.security.KeyStore.{PasswordProtection, PrivateKeyEntry}

import better.files._
import ciris._
import ciris.api._
import ciris.api.syntax._

import scala.util.{Failure, Success, Try}

package object kafka {
  implicit val aivenKafkaClientPrivateKeyConfigDecoder: ConfigDecoder[String, AivenKafkaClientPrivateKey] =
    ConfigDecoder.fromTry("AivenKafkaClientPrivateKey")(AivenKafkaClientPrivateKey.fromString)

  implicit val aivenKafkaClientCertificateConfigDecoder: ConfigDecoder[String, AivenKafkaClientCertificate] =
    ConfigDecoder.fromTry("AivenKafkaClientCertificate")(AivenKafkaClientCertificate.fromString)

  implicit val aivenKafkaServiceCertificateConfigDecoder: ConfigDecoder[String, AivenKafkaServiceCertificate] =
    ConfigDecoder.fromTry("AivenKafkaServiceCertificate")(AivenKafkaServiceCertificate.fromString)

  def aivenKafkaSetup[F[_]: Apply](
    clientPrivateKey: ConfigValue[F, AivenKafkaClientPrivateKey],
    clientCertificate: ConfigValue[F, AivenKafkaClientCertificate],
    serviceCertificate: ConfigValue[F, AivenKafkaServiceCertificate]
  ): ConfigValue[F, AivenKafkaSetupDetails] = ConfigValue.applyF {
    (clientPrivateKey.value product clientCertificate.value product serviceCertificate.value).map {
      case ((Right(clientPrivateKey), Right(clientCertificate)), Right(serviceCertificate)) =>
        setupKeyAndTrustStores(clientPrivateKey, clientCertificate, serviceCertificate)
      case ((clientPrivateKey, clientCertificate), serviceCertificate) =>
        Left {
          List(clientPrivateKey, clientCertificate, serviceCertificate)
            .collect { case Left(error) => error }
            .reduce(_ combine _)
        }
    }
  }

  private def setupKeyAndTrustStores(
    clientPrivateKey: AivenKafkaClientPrivateKey,
    clientCertificate: AivenKafkaClientCertificate,
    serviceCertificate: AivenKafkaServiceCertificate
  ): Either[ConfigError, AivenKafkaSetupDetails] = {
    val setupDetails =
      AivenKafkaSetupDetails.newTemporary()

    def keyStoreSetup() =
      setupKeyStore(
        clientPrivateKey = clientPrivateKey,
        clientCertificate = clientCertificate,
        keyStoreFile = setupDetails.keyStoreFile,
        keyStorePassword = setupDetails.keyStorePassword
      )

    def trustStoreSetup() =
      setupTrustStore(
        serviceCertificate = serviceCertificate,
        trustStoreFile = setupDetails.trustStoreFile,
        trustStorePassword = setupDetails.trustStorePassword
      )

    val setupStores =
      for {
        _ <- keyStoreSetup()
        _ <- trustStoreSetup()
      } yield ()

    setupStores match {
      case Success(_) =>
        Right(setupDetails)
      case Failure(cause) =>
        Left(ConfigError(s"Failed to setup Aiven Kafka key and trust stores: $cause"))
    }
  }

  private def setupStore(
    storeType: String,
    storeFile: File,
    storePasswordChars: Array[Char]
  )(setupStore: KeyStore => Unit): Try[Unit] = Try {
    val keyStore = KeyStore.getInstance(storeType)
    keyStore.load(null, storePasswordChars)
    setupStore(keyStore)

    for {
      outputStream <- storeFile.newOutputStream.autoClosed
    } keyStore.store(outputStream, storePasswordChars)
  }

  private def setupKeyStore(
    clientPrivateKey: AivenKafkaClientPrivateKey,
    clientCertificate: AivenKafkaClientCertificate,
    keyStoreFile: AivenKafkaKeyStoreFile,
    keyStorePassword: AivenKafkaKeyStorePassword
  ): Try[Unit] = {
    val keyStorePasswordChars =
      keyStorePassword.value.toCharArray

    setupStore(
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

  private def setupTrustStore(
    serviceCertificate: AivenKafkaServiceCertificate,
    trustStoreFile: AivenKafkaTrustStoreFile,
    trustStorePassword: AivenKafkaTrustStorePassword
  ): Try[Unit] = {
    setupStore(
      storeType = "JKS",
      storeFile = trustStoreFile.value,
      storePasswordChars = trustStorePassword.value.toCharArray
    ) { _.setCertificateEntry("CA", serviceCertificate.value) }
  }
}
