package ciris.aiven.kafka

import cats.implicits._
import ciris.{ConfigValue, Secret}

import java.util.UUID

sealed abstract class TrustStorePassword {
  def value: String
}

private[kafka] object TrustStorePassword {
  final def createTemporary[F[_]]: ConfigValue[F, TrustStorePassword] =
    ConfigValue.suspend {
      val _value = UUID.randomUUID().toString

      ConfigValue.default {
        new TrustStorePassword {
          override final val value: String =
            _value

          override final def toString: String =
            s"TrustStorePassword(${Secret(value).valueShortHash})"
        }
      }
    }
}
