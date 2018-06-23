## Ciris Aiven Kafka
[Aiven][aiven] Kafka support for [Ciris][ciris], using [better-files][better-files] to create the key and trust store files.

### Getting Started
To get started with [sbt][sbt], simply add the following lines to your `build.sbt` file.

```scala
resolvers += Resolver.bintrayRepo("ovotech", "maven")

libraryDependencies += "com.ovoenergy" %% "ciris-aiven-kafka" % "0.9"
```

The library is published for Scala 2.11 and 2.12.

### Usage
Simply `import ciris.aiven.kafka._` and use the `aivenKafkaSetup` function to setup the key and trust stores for the specified client private key, client certificate, and service certificate. The library provides support for reading the private key and certificates, so all you have to do is to say from which source they should be retrieved. The example below makes use of [`IO`][IO] from [cats-effect][cats-effect], but any effect type with a [`Sync`][Sync] instance available works with [`ciris-cats-effect`][ciris-cats-effect].

```scala
import cats.effect.IO
import ciris._
import ciris.aiven.kafka._
import ciris.cats.effect._
import ciris.syntax._

final case class Config(
  appName: String,
  kafkaSetup: AivenKafkaSetupDetails
)

val errorsOrConfig: IO[Either[ConfigErrors, Config]] =
  loadConfig(
    aivenKafkaSetup[IO](
      clientPrivateKey = fileWithNameSync("/tmp/service.key"), // Client private key type is inferred
      clientCertificate = fileWithNameSync("/tmp/service.cert"), // Client certificate type is inferred
      serviceCertificate = fileWithNameSync("/tmp/ca.pem") // Service certificate type is inferred
    )
  ) { kafkaSetup =>
    Config(
      appName = "my-service",
      kafkaSetup = kafkaSetup
    )
  }

val config: IO[Config] =
  errorsOrConfig.orRaiseThrowable
```

We've now described how to load the configuration, so let's try to actually load it. We're only using `unsafeRunSync` for demonstration purposes here, you should normally never use it.

```scala
config.unsafeRunSync()
// ciris.ConfigException: configuration loading failed with the following errors.
//
//   - Exception while reading file [(/tmp/service.key,UTF-8)]: java.io.FileNotFoundException: /tmp/service.key (No such file or directory) and exception while reading file [(/tmp/service.cert,UTF-8)]: java.io.FileNotFoundException: /tmp/service.cert (No such file or directory) and exception while reading file [(/tmp/ca.pem,UTF-8)]: java.io.FileNotFoundException: /tmp/ca.pem (No such file or directory).
//
//   at ciris.ConfigException$.apply(ConfigException.scala:34)
//   at ciris.ConfigErrors$.toException$extension(ConfigErrors.scala:128)
//   at ciris.syntax$EitherConfigErrorsFSyntax$.$anonfun$orRaiseThrowable$1(syntax.scala:71)
//   at cats.effect.internals.IORunLoop$.liftedTree3$1(IORunLoop.scala:207)
//   at cats.effect.internals.IORunLoop$.step(IORunLoop.scala:207)
//   at cats.effect.IO.unsafeRunTimed(IO.scala:307)
//   at cats.effect.IO.unsafeRunSync(IO.scala:242)
//   ... 36 elided
```

If the configuration loading was successful, `aivenKafkaSetup` will return an `AivenKafkaSetupDetails` with the key and trust store locations, and their passwords. Temporary files and passwords are used and the files are set to be deleted automatically on exit. The key store is of type PKCS12 and the trust store is of type JKS.

`AivenKafkaSetupDetails` provides a `setProperties` function to configure Kafka consumers and producers. For example, if you're using [Alpakka Kafka][alpakka-kafka], you can configure your consumers and producers as follows. Properties as `Map[String, String`] are also available with `properties`.

```scala
val kafkaSetup: AivenKafkaSetupDetails = ???

val settings: ConsumerSettings[Key, Value] = ???

// Using setProperties
kafkaSetup.setProperties(settings.withProperties)

// Using properties
settings.withProperties(kafkaSetup.properties)
```

[aiven]: https://aiven.io
[alpakka-kafka]: https://doc.akka.io/docs/akka-stream-kafka/current/home.html
[better-files]: https://github.com/pathikrit/better-files
[cats-effect]: https://typelevel.org/cats-effect/
[ciris-cats-effect]: https://cir.is/docs/cats-effect-module
[ciris]: https://cir.is
[IO]: https://typelevel.org/cats-effect/datatypes/io.html
[sbt]: https://www.scala-sbt.org
[Sync]: https://typelevel.org/cats-effect/typeclasses/sync.html
