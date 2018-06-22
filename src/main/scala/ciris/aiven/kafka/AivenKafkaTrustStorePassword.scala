package ciris.aiven.kafka

import java.util.UUID

import ciris.api.Sync

final class AivenKafkaTrustStorePassword private (val value: String) {
  override def toString: String =
    "AivenKafkaTrustStorePassword(***)"
}

object AivenKafkaTrustStorePassword {
  def newTemporary[F[_]](implicit F: Sync[F]): F[AivenKafkaTrustStorePassword] =
    F.suspend(F.pure(new AivenKafkaTrustStorePassword(UUID.randomUUID().toString)))
}
