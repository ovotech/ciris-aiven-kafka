package ciris.aiven.kafka

import java.util.UUID

final class AivenKafkaTrustStorePassword private (val value: String) {
  override def toString: String =
    "AivenKafkaTrustStorePassword(***)"
}

object AivenKafkaTrustStorePassword {
  def newTemporary(): AivenKafkaTrustStorePassword =
    new AivenKafkaTrustStorePassword(UUID.randomUUID().toString)
}
