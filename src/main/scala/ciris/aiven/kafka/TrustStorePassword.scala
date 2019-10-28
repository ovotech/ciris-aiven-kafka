package ciris.aiven.kafka

import cats.implicits._
import ciris.{ConfigValue, Secret}
import java.util.UUID

sealed abstract class TrustStorePassword {
  def value: String
}

private[kafka] final object TrustStorePassword {
  final val createTemporary: ConfigValue[TrustStorePassword] =
    ConfigValue.suspend {
      ConfigValue.default {
        new TrustStorePassword {
          override final val value: String =
            UUID.randomUUID().toString

          override final def toString: String =
            s"TrustStorePassword(${Secret(value).valueShortHash})"
        }
      }
    }
}
