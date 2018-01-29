package ciris.aiven.kafka

import better.files.File

sealed abstract case class AivenKafkaTrustStoreFile(value: File)

object AivenKafkaTrustStoreFile {
  def newTemporary(): AivenKafkaTrustStoreFile =
    new AivenKafkaTrustStoreFile({
      val file = File
        .newTemporaryFile("client.truststore-", ".jks")
        .delete(swallowIOExceptions = true)
      file.toJava.deleteOnExit()
      file
    }) {}
}
