package ciris.aiven.kafka

import java.security.spec.PKCS8EncodedKeySpec
import java.security.{KeyFactory, PrivateKey}
import java.util.Base64

import ciris.internal.digest.sha1Hex

import scala.util.Try

sealed abstract case class AivenKafkaClientPrivateKey(value: PrivateKey)

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
    }) {
      override def toString: String =
        s"AivenKafkaClientPrivateKey(${sha1Hex(privateKey).take(7)})"
    })
}
