package ciris.aiven.kafka

import better.files.File

import scala.util.Try

sealed abstract case class AivenKafkaTrustStoreFile(value: File)

object AivenKafkaTrustStoreFile {
  def newTemporary(): Try[AivenKafkaTrustStoreFile] =
    Try(new AivenKafkaTrustStoreFile({
      val file = File
        .newTemporaryFile("client.truststore-", ".jks")
        .delete(swallowIOExceptions = true)
      file.toJava.deleteOnExit()
      file
    }) {})
}
