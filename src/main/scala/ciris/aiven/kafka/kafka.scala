package ciris.aiven

import cats.implicits._
import ciris.ConfigValue
import java.nio.file.{Files, Path, StandardOpenOption}
import java.security.KeyStore

package object kafka {
  final def aivenKafkaSetup(
    clientPrivateKey: ConfigValue[String],
    clientCertificate: ConfigValue[String],
    serviceCertificate: ConfigValue[String]
  ): ConfigValue[AivenKafkaSetup] =
    (
      clientPrivateKey.secret.as[ClientPrivateKey],
      clientCertificate.secret.as[ClientCertificate],
      serviceCertificate.secret.as[ServiceCertificate]
    ).parTupled.flatMap((setupKeyAndTrustStores _).tupled)

  private[this] final def setupKeyAndTrustStores(
    clientPrivateKey: ClientPrivateKey,
    clientCertificate: ClientCertificate,
    serviceCertificate: ServiceCertificate
  ): ConfigValue[AivenKafkaSetup] =
    for {
      setupDetails <- AivenKafkaSetup.createTemporary
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
    } yield setupDetails

  private[this] final def setupStore(
    storeType: String,
    storePath: Path,
    storePasswordChars: Array[Char],
    setupStore: KeyStore => Unit
  ): ConfigValue[Unit] =
    ConfigValue.suspend {
      ConfigValue.default {
        val keyStore = KeyStore.getInstance(storeType)
        keyStore.load(null, storePasswordChars)
        setupStore(keyStore)

        val outputStream =
          Files.newOutputStream(storePath)

        try {
          keyStore.store(outputStream, storePasswordChars)
        } finally {
          outputStream.close()
        }
      }
    }

  private[this] final def setupKeyStore(
    clientPrivateKey: ClientPrivateKey,
    clientCertificate: ClientCertificate,
    keyStoreFile: KeyStoreFile,
    keyStorePassword: KeyStorePassword
  ): ConfigValue[Unit] = {
    val keyStorePasswordChars =
      keyStorePassword.value.toCharArray

    setupStore(
      storeType = "PKCS12",
      storePath = keyStoreFile.path,
      storePasswordChars = keyStorePasswordChars,
      setupStore = _.setEntry(
        "service_key",
        new KeyStore.PrivateKeyEntry(
          clientPrivateKey.value,
          Array(clientCertificate.value)
        ),
        new KeyStore.PasswordProtection(keyStorePasswordChars)
      )
    )
  }

  private[this] final def setupTrustStore(
    serviceCertificate: ServiceCertificate,
    trustStoreFile: TrustStoreFile,
    trustStorePassword: TrustStorePassword
  ): ConfigValue[Unit] =
    setupStore(
      storeType = "JKS",
      storePath = trustStoreFile.path,
      storePasswordChars = trustStorePassword.value.toCharArray,
      setupStore = _.setCertificateEntry("CA", serviceCertificate.value)
    )
}
