package ciris.aiven.kafka

import java.io.ByteArrayInputStream
import java.security.cert.{Certificate, CertificateFactory}

import scala.util.Try

final class AivenKafkaClientCertificate private (val value: Certificate) {
  override def toString: String =
    "AivenKafkaClientCertificate(***)"
}

object AivenKafkaClientCertificate {
  def fromString(certificate: String): Try[AivenKafkaClientCertificate] =
    Try(new AivenKafkaClientCertificate({
      val bais = new ByteArrayInputStream(certificate.getBytes("UTF-8"))
      CertificateFactory.getInstance("X.509").generateCertificate(bais)
    }))
}
