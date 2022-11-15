package ciris.aiven.kafka

import cats.implicits._
import ciris.{ConfigValue, Secret}

import java.util.UUID

sealed abstract class KeyStorePassword {
  def value: String
}

private[kafka] object KeyStorePassword {
  final def createTemporary[F[_]]: ConfigValue[F, KeyStorePassword] =
    ConfigValue.suspend {
      val _value = UUID.randomUUID().toString

      ConfigValue.default {
        new KeyStorePassword {
          override final val value: String =
            _value

          override final def toString: String =
            s"KeyStorePassword(${Secret(value).valueShortHash})"
        }
      }
    }
}
