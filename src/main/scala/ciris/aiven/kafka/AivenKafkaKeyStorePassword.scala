package ciris.aiven.kafka

import java.util.UUID

final class AivenKafkaKeyStorePassword private (val value: String) {
  override def toString: String =
    "AivenKafkaKeyStorePassword(***)"
}

object AivenKafkaKeyStorePassword {
  def newTemporary(): AivenKafkaKeyStorePassword =
    new AivenKafkaKeyStorePassword(UUID.randomUUID().toString)
}
