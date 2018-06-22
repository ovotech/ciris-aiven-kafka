package ciris.aiven.kafka

import better.files.File
import ciris.api.Sync
import ciris.api.syntax._

sealed abstract case class AivenKafkaKeyStoreFile(value: File)

object AivenKafkaKeyStoreFile {
  def newTemporary[F[_]](implicit F: Sync[F]): F[AivenKafkaKeyStoreFile] =
    F.suspend {
      F.pure {
        new AivenKafkaKeyStoreFile({
          val file = File
            .newTemporaryFile("client.keystore-", ".p12")
            .delete(swallowIOExceptions = true)
          file.toJava.deleteOnExit()
          file
        }) {}
      }
    }
}
