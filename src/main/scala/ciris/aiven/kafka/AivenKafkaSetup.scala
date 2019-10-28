package ciris.aiven.kafka

import ciris.ConfigValue

sealed abstract class AivenKafkaSetup {
  def keyStoreFile: KeyStoreFile

  def keyStorePassword: KeyStorePassword

  def trustStoreFile: TrustStoreFile

  def trustStorePassword: TrustStorePassword

  def properties: Map[String, String]
}

private[kafka] final object AivenKafkaSetup {
  final val createTemporary: ConfigValue[AivenKafkaSetup] =
    for {
      _keyStoreFile <- KeyStoreFile.createTemporary
      _keyStorePassword <- KeyStorePassword.createTemporary
      _trustStoreFile <- TrustStoreFile.createTemporary
      _trustStorePassword <- TrustStorePassword.createTemporary
    } yield {
      new AivenKafkaSetup {
        override final val keyStoreFile: KeyStoreFile =
          _keyStoreFile

        override final val keyStorePassword: KeyStorePassword =
          _keyStorePassword

        override final val trustStoreFile: TrustStoreFile =
          _trustStoreFile

        override final val trustStorePassword: TrustStorePassword =
          _trustStorePassword

        override final val properties: Map[String, String] =
          Map(
            "security.protocol" -> "SSL",
            "ssl.truststore.location" -> trustStoreFile.pathAsString,
            "ssl.truststore.password" -> trustStorePassword.value,
            "ssl.keystore.type" -> "PKCS12",
            "ssl.keystore.location" -> keyStoreFile.pathAsString,
            "ssl.keystore.password" -> keyStorePassword.value,
            "ssl.key.password" -> keyStorePassword.value
          )

        override final def toString: String =
          s"AivenKafkaSetup($keyStoreFile, $keyStorePassword, $trustStoreFile, $trustStorePassword)"
      }
    }
}
