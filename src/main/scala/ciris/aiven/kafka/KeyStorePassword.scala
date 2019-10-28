package ciris.aiven.kafka

import cats.implicits._
import ciris.{ConfigValue, Secret}
import java.util.UUID

sealed abstract class KeyStorePassword {
  def value: String
}

private[kafka] final object KeyStorePassword {
  final val createTemporary: ConfigValue[KeyStorePassword] =
    ConfigValue.suspend {
      ConfigValue.default {
        new KeyStorePassword {
          override final val value: String =
            UUID.randomUUID().toString

          override final def toString: String =
            s"KeyStorePassword(${Secret(value).valueShortHash})"
        }
      }
    }
}
