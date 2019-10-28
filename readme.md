## Ciris Aiven Kafka

[Aiven](https://aiven.io) Kafka support for [Ciris](https://cir.is).

### Getting Started

To get started with [sbt](https://www.scala-sbt.org), simply add the following lines to your `build.sbt` file.

```scala
resolvers += Resolver.bintrayRepo("ovotech", "maven")

libraryDependencies += "com.ovoenergy" %% "ciris-aiven-kafka" % "1.0.0"
```

The library is published for Scala 2.12 and 2.13.

### Usage

Simply `import ciris.aiven.kafka._` and use `aivenKafkaSetup` to setup the key and trust stores.

```scala
import ciris._
import ciris.aiven.kafka._

val kafkaSetup: ConfigValue[AivenKafkaSetup] =
  aivenKafkaSetup(
    clientPrivateKey = env("CLIENT_PRIVATE_KEY"),
    clientCertificate = env("CLIENT_CERTIFICATE"),
    serviceCertificate = env("SERVICE_CERTIFICATE")
  )
```

If the configuration loading was successful, `aivenKafkaSetup` will return an `AivenKafkaSetup` with the key and trust store locations, and their passwords. Temporary files and passwords are used and the files are set to be deleted automatically on exit. The key store is of type PKCS12 and the trust store is of type JKS.

`AivenKafkaSetup` provides `properties: Map[String, String]` with suitable consumer properties.
