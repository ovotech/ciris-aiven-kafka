package ciris.aiven.kafka

import ciris.ConfigValue
import java.nio.file.{Files, Path}

sealed abstract class KeyStoreFile {
  def path: Path

  def pathAsString: String
}

private[kafka] final object KeyStoreFile {
  final val createTemporary: ConfigValue[KeyStoreFile] =
    ConfigValue.suspend {
      ConfigValue.default {
        new KeyStoreFile {
          override final val path: Path = {
            val path = Files.createTempFile("client.keystore-", ".p12")
            path.toFile.deleteOnExit()
            Files.delete(path)
            path
          }

          override final def pathAsString: String =
            path.toString

          override final def toString: String =
            s"KeyStoreFile($pathAsString)"
        }
      }
    }
}
