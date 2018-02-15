package ciris.aiven.kafka

import scala.util.Try

sealed abstract case class AivenKafkaSetupDetails(
  keyStoreFile: AivenKafkaKeyStoreFile,
  keyStorePassword: AivenKafkaKeyStorePassword,
  trustStoreFile: AivenKafkaTrustStoreFile,
  trustStorePassword: AivenKafkaTrustStorePassword
) {

  private class WithProperty[A](a: A)(f: (A, String, String) => A) {
    def withProperty(key: String, value: String): WithProperty[A] =
      new WithProperty(f(a, key, value))(f)

    def value: A = a
  }

  val properties: Map[String, String] =
    Map(
      "security.protocol" -> "SSL",
      "ssl.truststore.location" -> trustStoreFile.value.pathAsString,
      "ssl.truststore.password" -> trustStorePassword.value,
      "ssl.keystore.type" -> "PKCS12",
      "ssl.keystore.location" -> keyStoreFile.value.pathAsString,
      "ssl.keystore.password" -> keyStorePassword.value,
      "ssl.key.password" -> keyStorePassword.value
    )

  def setProperties[A](a: A)(f: (A, String, String) => A): A = {
    properties.toList
      .foldLeft(new WithProperty(a)(f)) {
        case (a, (key, value)) =>
          a.withProperty(key, value)
      }
      .value
  }

  def setProperties[A](f: Map[String, String] => A): A =
    f(properties)
}

object AivenKafkaSetupDetails {
  def newTemporary(): Try[AivenKafkaSetupDetails] =
    for {
      keyStoreFile <- AivenKafkaKeyStoreFile.newTemporary()
      trustStoreFile <- AivenKafkaTrustStoreFile.newTemporary()
    } yield {
      new AivenKafkaSetupDetails(
        keyStoreFile,
        AivenKafkaKeyStorePassword.newTemporary(),
        trustStoreFile,
        AivenKafkaTrustStorePassword.newTemporary()
      ) {}
    }
}
