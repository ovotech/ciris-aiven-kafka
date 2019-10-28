package ciris.aiven.kafka

import ciris.{ConfigDecoder, Secret}
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.cert.{Certificate, CertificateException, CertificateFactory}

sealed abstract class ClientCertificate {
  def value: Certificate
}

private[kafka] final object ClientCertificate {
  final def apply(clientCertificate: Secret[String]): Option[ClientCertificate] =
    try {
      Some {
        new ClientCertificate {
          override final val value: Certificate =
            CertificateFactory
              .getInstance("X.509")
              .generateCertificate {
                new ByteArrayInputStream(
                  clientCertificate.value.getBytes(StandardCharsets.UTF_8)
                )
              }

          override final def toString: String =
            s"ClientCertificate(${clientCertificate.valueShortHash})"
        }
      }
    } catch {
      case _: CertificateException =>
        None
    }

  // format: off
  implicit final val secretStringClientCertificateConfigDecoder: ConfigDecoder[Secret[String], ClientCertificate] =
    ConfigDecoder.identity[Secret[String]].mapOption("ClientCertificate")(apply)
}
