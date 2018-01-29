package ciris.aiven.kafka

import java.io.ByteArrayInputStream
import java.security.cert.{Certificate, CertificateFactory}

import scala.util.Try

final class AivenKafkaServiceCertificate private (val value: Certificate) {
  override def toString: String =
    "AivenKafkaServiceCertificate(***)"
}

object AivenKafkaServiceCertificate {
  def fromString(certificate: String): Try[AivenKafkaServiceCertificate] =
    Try(new AivenKafkaServiceCertificate({
      val bais = new ByteArrayInputStream(certificate.getBytes("UTF-8"))
      CertificateFactory.getInstance("X.509").generateCertificate(bais)
    }))
}
