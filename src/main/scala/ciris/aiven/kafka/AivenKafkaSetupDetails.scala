package ciris.aiven.kafka

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

  def setProperties[A](a: A)(f: (A, String, String) => A): A = {
    new WithProperty(a)(f)
      .withProperty("security.protocol", "SSL")
      .withProperty("ssl.truststore.location", trustStoreFile.value.pathAsString)
      .withProperty("ssl.truststore.password", trustStorePassword.value)
      .withProperty("ssl.keystore.type", "PKCS12")
      .withProperty("ssl.keystore.location", keyStoreFile.value.pathAsString)
      .withProperty("ssl.keystore.password", keyStorePassword.value)
      .withProperty("ssl.key.password", keyStorePassword.value)
      .value
  }
}

object AivenKafkaSetupDetails {
  def newTemporary(): AivenKafkaSetupDetails =
    new AivenKafkaSetupDetails(
      AivenKafkaKeyStoreFile.newTemporary(),
      AivenKafkaKeyStorePassword.newTemporary(),
      AivenKafkaTrustStoreFile.newTemporary(),
      AivenKafkaTrustStorePassword.newTemporary()
    ) {}
}
