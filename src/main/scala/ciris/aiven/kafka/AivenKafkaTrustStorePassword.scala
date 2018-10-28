package ciris.aiven.kafka

import java.util.UUID

import ciris.api.Sync
import ciris.internal.digest.sha1Hex

sealed abstract case class AivenKafkaTrustStorePassword(value: String)

object AivenKafkaTrustStorePassword {
  def newTemporary[F[_]](implicit F: Sync[F]): F[AivenKafkaTrustStorePassword] =
    F.suspend(F.pure(new AivenKafkaTrustStorePassword(UUID.randomUUID().toString) {
      override def toString: String =
        s"AivenKafkaTrustStorePassword(${sha1Hex(value).take(7)})"
    }))
}
