package ciris.aiven.kafka

import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, PrivateKey}
import java.util.Base64

import scala.util.Try

final class AivenKafkaClientPrivateKey private (val value: PrivateKey) {
  override def toString: String =
    "AivenKafkaClientPrivateKey(***)"
}

object AivenKafkaClientPrivateKey {
  def fromString(privateKey: String): Try[AivenKafkaClientPrivateKey] =
    Try(new AivenKafkaClientPrivateKey({
      val rawPrivateKey =
        privateKey
          .replace("-----BEGIN PRIVATE KEY-----", "")
          .replace("-----END PRIVATE KEY-----", "")
          .filterNot(_.isWhitespace)

      val rawServiceKeyDecoded = Base64.getDecoder.decode(rawPrivateKey)
      val serviceKeySpecification = new PKCS8EncodedKeySpec(rawServiceKeyDecoded)
      KeyFactory.getInstance("RSA").generatePrivate(serviceKeySpecification)
    }))
}
