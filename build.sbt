import Dependencies._

name := "kafka-stream-tests"
version := "0.1"

lazy val commonSettings = Seq(
  scalaVersion := "2.13.8",
  resolvers += "Confluent Maven Repository" at "https://packages.confluent.io/maven/"
)

lazy val root = (project in file("."))
  .settings(commonSettings: _*)
  .settings(
    libraryDependencies ++= Seq(
      Libs.scalaTest % Test
    )
  )
  .aggregate(
    carDataProducer,
    carDataConsumer,
    driverNotifier,
    avro,
    domain
  )

lazy val carDataProducer = (project in file("car-data-producer"))
  .settings(commonSettings)
  .settings(
    name := "car-data-producer",
    libraryDependencies ++= Seq(
      Libs.kafkaClient,
      Libs.kafkaAvro,
      Libs.catsEffect,
      Libs.scalaTest % Test
    )
  )
  .dependsOn(domain, avro)

lazy val carDataConsumer = (project in file("car-data-consumer"))
  .settings(commonSettings)
  .settings(
    name := "car-data-consumer",
    libraryDependencies ++= Seq(
      Libs.kafkaClient,
      Libs.kafkaAvro,
      Libs.catsEffect,
      Libs.scalaTest % Test
    )
  )
  .dependsOn(domain, avro)

lazy val driverNotifier = (project in file("driver-notifier"))
  .settings(commonSettings)
  .settings(
    name := "driver-notifier",
    libraryDependencies ++= Seq(
      Libs.kafkaStreamsScala,
      Libs.kafkaStreamsAvro,
      Libs.avro4sKafka,
      Libs.scalaTest % Test
    )
  )
  .dependsOn(domain, avro)

lazy val avro = (project in file("avro"))
  .settings(commonSettings)
  .settings(
    name := "avro",
    libraryDependencies ++= Seq(
      Libs.avro4sCore,
      Libs.sttp3Core,
      Libs.sttp3Circe,
      Libs.circeGeneric,
      Libs.smlTagging,
      Libs.scalaTest % Test
    )
  )
  .dependsOn(domain)

lazy val domain = (project in file("domain")).settings(commonSettings)