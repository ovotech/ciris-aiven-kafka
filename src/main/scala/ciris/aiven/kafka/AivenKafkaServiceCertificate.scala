package ciris.aiven.kafka

import java.io.ByteArrayInputStream
import java.security.cert.{Certificate, CertificateFactory}

import ciris.internal.digest.sha1Hex

import scala.util.Try

sealed abstract case class AivenKafkaServiceCertificate(value: Certificate)

object AivenKafkaServiceCertificate {
  def fromString(certificate: String): Try[AivenKafkaServiceCertificate] =
    Try(new AivenKafkaServiceCertificate({
      val bais = new ByteArrayInputStream(certificate.getBytes("UTF-8"))
      CertificateFactory.getInstance("X.509").generateCertificate(bais)
    }) {
      override def toString: String =
        s"AivenKafkaServiceCertificate(${sha1Hex(certificate).take(7)})"
    })
}
