package ciris.aiven.kafka

import better.files.File
import ciris.api.Sync
import ciris.api.syntax._

sealed abstract case class AivenKafkaTrustStoreFile(value: File)

object AivenKafkaTrustStoreFile {
  def newTemporary[F[_]](implicit F: Sync[F]): F[AivenKafkaTrustStoreFile] =
    F.suspend {
      F.pure {
        new AivenKafkaTrustStoreFile({
          val file = File
            .newTemporaryFile("client.truststore-", ".jks")
            .delete(swallowIOExceptions = true)
          file.toJava.deleteOnExit()
          file
        }) {}
      }
    }
}
