package ciris.aiven.kafka

import java.util.UUID

import ciris.api.Sync
import ciris.internal.digest.sha1Hex

sealed abstract case class AivenKafkaKeyStorePassword(value: String)

object AivenKafkaKeyStorePassword {
  def newTemporary[F[_]](implicit F: Sync[F]): F[AivenKafkaKeyStorePassword] =
    F.suspend(F.pure(new AivenKafkaKeyStorePassword(UUID.randomUUID().toString) {
      override def toString: String =
        s"AivenKafkaKeyStorePassword(${sha1Hex(value).take(7)})"
    }))
}
