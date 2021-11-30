package ciris.aiven.kafka

import ciris.ConfigValue
import java.nio.file.{Files, Path}

sealed abstract class KeyStoreFile {
  def path: Path

  def pathAsString: String
}

private[kafka] object KeyStoreFile {
  final def createTemporary[F[_]]: ConfigValue[F, KeyStoreFile] =
    ConfigValue.suspend {
      val _path = {
        val path = Files.createTempFile("client.keystore-", ".p12")
        path.toFile.deleteOnExit()
        Files.delete(path)
        path
      }

      ConfigValue.default {
        new KeyStoreFile {
          override final val path: Path =
            _path

          override final def pathAsString: String =
            path.toString

          override final def toString: String =
            s"KeyStoreFile($pathAsString)"
        }
      }
    }
}
