package ciris.aiven.kafka

import java.io.ByteArrayInputStream
import java.security.cert.{Certificate, CertificateFactory}

import ciris.internal.digest.sha1Hex

import scala.util.Try

sealed abstract case class AivenKafkaClientCertificate(value: Certificate)

object AivenKafkaClientCertificate {
  def fromString(certificate: String): Try[AivenKafkaClientCertificate] =
    Try(new AivenKafkaClientCertificate({
      val bais = new ByteArrayInputStream(certificate.getBytes("UTF-8"))
      CertificateFactory.getInstance("X.509").generateCertificate(bais)
    }) {
      override def toString: String =
        s"AivenKafkaClientCertificate(${sha1Hex(certificate).take(7)})"
    })
}
