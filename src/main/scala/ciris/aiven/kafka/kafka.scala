package ciris.aiven

import cats.effect.Blocker
import cats.implicits._
import ciris.ConfigValue

import java.nio.file.{Files, Path}
import java.security.KeyStore

package object kafka {
  final def aivenKafkaSetup(
    clientPrivateKey: ConfigValue[String],
    clientCertificate: ConfigValue[String],
    serviceCertificate: ConfigValue[String],
    blocker: Blocker
  ): ConfigValue[AivenKafkaSetup] =
    (
      clientPrivateKey.secret.as[ClientPrivateKey],
      clientCertificate.secret.as[ClientCertificate],
      serviceCertificate.secret.as[ServiceCertificate]
    ).parTupled.flatMap {
      case (clientPrivateKey, clientCertificate, serviceCertificate) =>
        setupKeyAndTrustStores(
          clientPrivateKey = clientPrivateKey,
          clientCertificate = clientCertificate,
          serviceCertificate = serviceCertificate,
          blocker = blocker
        )
    }

  private[this] final def setupKeyAndTrustStores(
    clientPrivateKey: ClientPrivateKey,
    clientCertificate: ClientCertificate,
    serviceCertificate: ServiceCertificate,
    blocker: Blocker
  ): ConfigValue[AivenKafkaSetup] =
    for {
      setupDetails <- AivenKafkaSetup.createTemporary
      _ <- setupKeyStore(
        clientPrivateKey = clientPrivateKey,
        clientCertificate = clientCertificate,
        keyStoreFile = setupDetails.keyStoreFile,
        keyStorePassword = setupDetails.keyStorePassword,
        blocker = blocker
      )
      _ <- setupTrustStore(
        serviceCertificate = serviceCertificate,
        trustStoreFile = setupDetails.trustStoreFile,
        trustStorePassword = setupDetails.trustStorePassword,
        blocker = blocker
      )
    } yield setupDetails

  private[this] final def setupStore(
    storeType: String,
    storePath: Path,
    storePasswordChars: Array[Char],
    setupStore: KeyStore => Unit,
    blocker: Blocker
  ): ConfigValue[Unit] =
    ConfigValue.blockOn(blocker) {
      ConfigValue.suspend {
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

        ConfigValue.default(())
      }
    }

  private[this] final def setupKeyStore(
    clientPrivateKey: ClientPrivateKey,
    clientCertificate: ClientCertificate,
    keyStoreFile: KeyStoreFile,
    keyStorePassword: KeyStorePassword,
    blocker: Blocker
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
      ),
      blocker = blocker
    )
  }

  private[this] final def setupTrustStore(
    serviceCertificate: ServiceCertificate,
    trustStoreFile: TrustStoreFile,
    trustStorePassword: TrustStorePassword,
    blocker: Blocker
  ): ConfigValue[Unit] =
    setupStore(
      storeType = "JKS",
      storePath = trustStoreFile.path,
      storePasswordChars = trustStorePassword.value.toCharArray,
      setupStore = _.setCertificateEntry("CA", serviceCertificate.value),
      blocker = blocker
    )
}
