package ciris.aiven

import java.security.KeyStore
import java.security.KeyStore.{PasswordProtection, PrivateKeyEntry}

import better.files._
import ciris.{ConfigError, ConfigReader, ConfigValue}

import scala.util.{Failure, Success, Try}

package object kafka {
  implicit val aivenKafkaClientPrivateKeyConfigReader: ConfigReader[AivenKafkaClientPrivateKey] =
    ConfigReader.fromTry("AivenClientPrivateKey")(AivenKafkaClientPrivateKey.fromString)

  implicit val aivenKafkaClientCertificateConfigReader: ConfigReader[AivenKafkaClientCertificate] =
    ConfigReader.fromTry("AivenClientCertificate")(AivenKafkaClientCertificate.fromString)

  def aivenKafkaSetup(
    privateKey: ConfigValue[AivenKafkaClientPrivateKey],
    certificate: ConfigValue[AivenKafkaClientCertificate]
  ): ConfigValue[AivenKafkaSetupDetails] = ConfigValue {
    (privateKey.value, certificate.value) match {
      case (Right(privateKey), Right(certificate)) =>
        setupKeyAndTrustStores(privateKey, certificate)
      case (Left(error1), Left(error2)) => Left(error1 combine error2)
      case (Left(error1), Right(_))     => Left(error1)
      case (Right(_), Left(error2))     => Left(error2)
    }
  }

  private def setupKeyAndTrustStores(
    privateKey: AivenKafkaClientPrivateKey,
    certificate: AivenKafkaClientCertificate
  ): Either[ConfigError, AivenKafkaSetupDetails] = {
    val setupDetails =
      AivenKafkaSetupDetails.newTemporary()

    def keyStoreSetup() =
      setupKeyStore(
        privateKey = privateKey,
        certificate = certificate,
        keyStoreFile = setupDetails.keyStoreFile,
        keyStorePassword = setupDetails.keyStorePassword
      )

    def trustStoreSetup() =
      setupTrustStore(
        certificate = certificate,
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
    privateKey: AivenKafkaClientPrivateKey,
    certificate: AivenKafkaClientCertificate,
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
          privateKey.value,
          Array(certificate.value)
        ),
        new PasswordProtection(keyStorePasswordChars)
      )
    }
  }

  private def setupTrustStore(
    certificate: AivenKafkaClientCertificate,
    trustStoreFile: AivenKafkaTrustStoreFile,
    trustStorePassword: AivenKafkaTrustStorePassword
  ): Try[Unit] = {
    setupStore(
      storeType = "JKS",
      storeFile = trustStoreFile.value,
      storePasswordChars = trustStorePassword.value.toCharArray
    ) { _.setCertificateEntry("CA", certificate.value) }
  }
}
