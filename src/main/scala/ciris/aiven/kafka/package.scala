package ciris.aiven

import java.security.KeyStore
import java.security.KeyStore.{PasswordProtection, PrivateKeyEntry}

import better.files._
import ciris.{ConfigError, ConfigReader, ConfigValue}

import scala.util.{Failure, Success, Try}

package object kafka {
  implicit val aivenKafkaClientPrivateKeyConfigReader: ConfigReader[AivenKafkaClientPrivateKey] =
    ConfigReader.fromTry("AivenKafkaClientPrivateKey")(AivenKafkaClientPrivateKey.fromString)

  implicit val aivenKafkaClientCertificateConfigReader: ConfigReader[AivenKafkaClientCertificate] =
    ConfigReader.fromTry("AivenKafkaClientCertificate")(AivenKafkaClientCertificate.fromString)

  implicit val aivenKafkaServiceCertificateConfigReader: ConfigReader[AivenKafkaServiceCertificate] =
    ConfigReader.fromTry("AivenKafkaServiceCertificate")(AivenKafkaServiceCertificate.fromString)

  def aivenKafkaSetup(
    clientPrivateKey: ConfigValue[AivenKafkaClientPrivateKey],
    clientCertificate: ConfigValue[AivenKafkaClientCertificate],
    serviceCertificate: ConfigValue[AivenKafkaServiceCertificate]
  ): ConfigValue[AivenKafkaSetupDetails] = ConfigValue {
    (clientPrivateKey.value, clientCertificate.value, serviceCertificate.value) match {
      case (Right(clientPrivateKey), Right(clientCertificate), Right(serviceCertificate)) =>
        setupKeyAndTrustStores(clientPrivateKey, clientCertificate, serviceCertificate)
      case _ =>
        Left {
          List(clientPrivateKey.value, clientCertificate.value, serviceCertificate.value)
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
