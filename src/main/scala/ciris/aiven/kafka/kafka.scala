package ciris.aiven

import cats.implicits._
import ciris.ConfigValue

import java.nio.file.{Files, Path}
import java.security.KeyStore

package object kafka {
  final def aivenKafkaSetup[F[_]](
    clientPrivateKey: ConfigValue[F, String],
    clientCertificate: ConfigValue[F, String],
    serviceCertificate: ConfigValue[F, String]
  ): ConfigValue[F, AivenKafkaSetup] =
    (
      clientPrivateKey.secret.as[ClientPrivateKey],
      clientCertificate.secret.as[ClientCertificate],
      serviceCertificate.secret.as[ServiceCertificate]
    ).parTupled.flatMap {
      case (clientPrivateKey, clientCertificate, serviceCertificate) =>
        setupKeyAndTrustStores(
          clientPrivateKey = clientPrivateKey,
          clientCertificate = clientCertificate,
          serviceCertificate = serviceCertificate
        )
    }

  private[this] final def setupKeyAndTrustStores[F[_]](
    clientPrivateKey: ClientPrivateKey,
    clientCertificate: ClientCertificate,
    serviceCertificate: ServiceCertificate
  ): ConfigValue[F, AivenKafkaSetup] =
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

  private[this] final def setupStore[F[_]](
    storeType: String,
    storePath: Path,
    storePasswordChars: Array[Char],
    setupStore: KeyStore => Unit
  ): ConfigValue[F, Unit] =
    ConfigValue.blocking {
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

  private[this] final def setupKeyStore[F[_]](
    clientPrivateKey: ClientPrivateKey,
    clientCertificate: ClientCertificate,
    keyStoreFile: KeyStoreFile,
    keyStorePassword: KeyStorePassword
  ): ConfigValue[F, Unit] = {
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

  private[this] final def setupTrustStore[F[_]](
    serviceCertificate: ServiceCertificate,
    trustStoreFile: TrustStoreFile,
    trustStorePassword: TrustStorePassword
  ): ConfigValue[F, Unit] =
    setupStore(
      storeType = "JKS",
      storePath = trustStoreFile.path,
      storePasswordChars = trustStorePassword.value.toCharArray,
      setupStore = _.setCertificateEntry("CA", serviceCertificate.value)
    )
}
