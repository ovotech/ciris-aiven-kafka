package ciris.aiven.kafka

import ciris.{ConfigDecoder, Secret}
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.{Certificate, CertificateException, CertificateFactory}

sealed abstract class ServiceCertificate {
  def value: Certificate
}

private[kafka] object ServiceCertificate {
  final def apply(serviceCertificate: Secret[String]): Option[ServiceCertificate] =
    try {
      Some {
        new ServiceCertificate {
          override final val value: Certificate =
            CertificateFactory
              .getInstance("X.509")
              .generateCertificate {
                new ByteArrayInputStream(
                  serviceCertificate.value.getBytes(StandardCharsets.UTF_8)
                )
              }

          override final def toString: String =
            s"ServiceCertificate(${serviceCertificate.valueShortHash})"
        }
      }
    } catch {
      case _: CertificateException =>
        None
    }

  // format: off
  implicit final val secretStringServiceCertificateConfigDecoder: ConfigDecoder[Secret[String], ServiceCertificate] =
    ConfigDecoder.identity[Secret[String]].mapOption("ServiceCertificate")(apply)
}
