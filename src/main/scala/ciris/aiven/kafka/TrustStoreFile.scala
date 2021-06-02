package ciris.aiven.kafka

import ciris.ConfigValue
import java.nio.file.{Files, Path}

sealed abstract class TrustStoreFile {
  def path: Path

  def pathAsString: String
}

private[kafka] final object TrustStoreFile {
  final def createTemporary[F[_]]: ConfigValue[F, TrustStoreFile] =
    ConfigValue.suspend {
      val _path = {
        val path = Files.createTempFile("client.truststore-", ".jks")
        path.toFile.deleteOnExit()
        Files.delete(path)
        path
      }

      ConfigValue.default {
        new TrustStoreFile {
          override final val path: Path =
            _path

          override final def pathAsString: String =
            path.toString

          override final def toString: String =
            s"TrustStoreFile($pathAsString)"
        }
      }
    }
}
