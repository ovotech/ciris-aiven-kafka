package ciris.aiven.kafka

import better.files.File

sealed abstract case class AivenKafkaKeyStoreFile(value: File)

object AivenKafkaKeyStoreFile {
  def newTemporary(): AivenKafkaKeyStoreFile =
    new AivenKafkaKeyStoreFile({
      val file = File
        .newTemporaryFile("client.keystore-", ".p12")
        .delete(swallowIOExceptions = true)
      file.toJava.deleteOnExit()
      file
    }) {}
}
