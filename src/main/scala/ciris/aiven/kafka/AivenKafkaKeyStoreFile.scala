package ciris.aiven.kafka

import better.files.File

import scala.util.Try

sealed abstract case class AivenKafkaKeyStoreFile(value: File)

object AivenKafkaKeyStoreFile {
  def newTemporary(): Try[AivenKafkaKeyStoreFile] =
    Try(new AivenKafkaKeyStoreFile({
      val file = File
        .newTemporaryFile("client.keystore-", ".p12")
        .delete(swallowIOExceptions = true)
      file.toJava.deleteOnExit()
      file
    }) {})
}
