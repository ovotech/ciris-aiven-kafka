package ciris.aiven.kafka

import java.util.UUID

import ciris.api.Sync

final class AivenKafkaKeyStorePassword private (val value: String) {
  override def toString: String =
    "AivenKafkaKeyStorePassword(***)"
}

object AivenKafkaKeyStorePassword {
  def newTemporary[F[_]](implicit F: Sync[F]): F[AivenKafkaKeyStorePassword] =
    F.suspend(F.pure(new AivenKafkaKeyStorePassword(UUID.randomUUID().toString)))
}
