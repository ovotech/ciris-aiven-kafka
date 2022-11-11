package ciris.aiven.kafka

import ciris.{ConfigDecoder, Secret}

import java.nio.charset.StandardCharsets
import java.security.spec.{InvalidKeySpecException, PKCS8EncodedKeySpec}
import java.security.{KeyFactory, NoSuchAlgorithmException, PrivateKey}
import java.util.Base64

sealed abstract class ClientPrivateKey {
  def value: PrivateKey
}

private[kafka] object ClientPrivateKey {
  final def apply(clientPrivateKey: Secret[String]): Option[ClientPrivateKey] =
    try {
      Some {
        new ClientPrivateKey {
          override final val value: PrivateKey =
            KeyFactory
              .getInstance("RSA")
              .generatePrivate {
                new PKCS8EncodedKeySpec(
                  Base64.getDecoder.decode {
                    clientPrivateKey.value
                      .replace("-----BEGIN PRIVATE KEY-----", "")
                      .replace("-----END PRIVATE KEY-----", "")
                      .filterNot(_.isWhitespace)
                      .getBytes(StandardCharsets.UTF_8)
                  }
                )
              }

          override final def toString: String =
            s"ClientPrivateKey(${clientPrivateKey.valueShortHash})"
        }
      }
    } catch {
      case _: NoSuchAlgorithmException | _: InvalidKeySpecException =>
        None
    }

  // format: off
  implicit final val secretStringClientPrivateKeyConfigDecoder: ConfigDecoder[Secret[String], ClientPrivateKey] =
    ConfigDecoder.identity[Secret[String]].mapOption("ClientPrivateKey")(apply)
}
