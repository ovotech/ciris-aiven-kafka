## Ciris Aiven Kafka
[Aiven][aiven] Kafka support for [Ciris][ciris], using [better-files][better-files] to create the key and trust store files.

### Getting Started
To get started with [SBT][sbt], simply add the following lines to your `build.sbt` file.

```scala
resolvers += Resolver.bintrayRepo("ovotech", "maven")

libraryDependencies += "com.ovoenergy" %% "ciris-aiven-kafka" % "0.3"
```

The library is published for Scala 2.11 and 2.12.

### Usage
Simply `import ciris.aiven.kafka._` and use the `aivenKafkaSetup` function to setup the key and trust store for the specified client private key, client certificate, and service certificate. The library provides support for reading the private key and certificates, so all you have to do is to say from which source they should be retrieved.

```scala
import ciris._
import ciris.syntax._
import ciris.aiven.kafka._

final case class Config(
  appName: String,
  kafkaSetup: AivenKafkaSetupDetails
)

val config =
  loadConfig(
    aivenKafkaSetup(
      clientPrivateKey = fileWithName("/tmp/service.key"), // Client private key type is inferred
      clientCertificate = fileWithName("/tmp/service.cert"), // Client certificate type is inferred
      serviceCertificate = fileWithName("/tmp/ca.pem") // Service certificate type is inferred
    )
  ) { kafkaSetup =>
    Config(
      appName = "my-service",
      kafkaSetup = kafkaSetup
    )
  }
```

The `loadConfig` method returns an `Either[ConfigErrors, Config]`, and we can quickly take a look at the errors with `config.orThrow()`, like in the following example.

```scala
config.orThrow()
// ciris.ConfigException: configuration loading failed with the following errors.
//
//   - Exception while reading file [(/tmp/service.key,UTF-8)]: java.io.FileNotFoundException: /tmp/service.key (No such file or directory)
//
//   at ciris.ConfigException$.apply(ConfigException.scala:33)
//   at ciris.ConfigErrors$.toException$extension(ConfigErrors.scala:109)
//   at ciris.syntax$EitherConfigErrorsSyntax$.$anonfun$orThrow$1(syntax.scala:22)
//   at ciris.syntax$EitherConfigErrorsSyntax$.$anonfun$orThrow$1$adapted(syntax.scala:22)
//   at scala.util.Either.fold(Either.scala:189)
//   at ciris.syntax$EitherConfigErrorsSyntax$.orThrow$extension(syntax.scala:23)
//   ... 43 elided
```

If the configuration loading was successful, `aivenKafkaSetup` will return an `AivenKafkaSetupDetails` with the key and trust store locations, and their passwords. Temporary files and passwords are used and the files are set to be deleted automatically on exit. The key store is of type PKCS12 and the trust store is of type JKS.

`AivenKafkaSetupDetails` provides a `setProperties` function to configure Kafka consumers and producers. For example, if you're using [Akka Streams Kafka][akka-streams-kafka], you can configure your consumers and producers like in the following example. You can also retrieve the Kafka properties as a `Map[String, String]` with `properties`.

```scala
val kafkaSetup: AivenKafkaSetupDetails = ???

val settings: ConsumerSettings[Key, Value] = ???

// Using setProperties
kafkaSetup.setProperties(settings.withProperties)

// Using properties
settings.withProperties(kafkaSetup.properties)
```

[aiven]: https://aiven.io
[akka-streams-kafka]: https://doc.akka.io/docs/akka-stream-kafka/current/home.html
[better-files]: https://github.com/pathikrit/better-files
[ciris]: https://cir.is
[sbt]: https://www.scala-sbt.org
