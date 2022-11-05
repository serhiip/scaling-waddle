import sbt._

object Dependencies {

  object V {
    val kafka = "7.2.2-ce"
    val kafkaAvro = "7.2.2"
    val avro4s = "4.1.0"
    val sttp3 = "3.3.11"
    val circe = "0.14.1"
    val cats = "3.2.5"
    val smlCommon = "2.3.1"
    val scalaTest = "3.2.13"
    val confluentUtils = "7.2.2"
    val logback = "1.4.4"
    val slf4j = "2.0.3"
    val catslogging = "2.5.0"
    val grpcNetty = "1.50.2"
    val grpcServices = "1.50.2"
  }

  object Libs {
    val kafkaClient = "org.apache.kafka" % "kafka-clients" % V.kafka
    val kafkaStreams = "org.apache.kafka" % "kafka-streams" % V.kafka
    val kafkaStreamsScala = "org.apache.kafka" %% "kafka-streams-scala" % V.kafka
    val kafkaAvro = "io.confluent" % "kafka-avro-serializer" % V.kafkaAvro
    val kafkaStreamsAvro = "io.confluent" % "kafka-streams-avro-serde" % V.kafkaAvro

    val avro4sCore = "com.sksamuel.avro4s" % "avro4s-core_2.13" % V.avro4s
    val avro4sKafka = "com.sksamuel.avro4s" % "avro4s-kafka_2.13" % V.avro4s
    val confluentUtils = "io.confluent" % "common-utils" % V.confluentUtils

    val sttp3Core = "com.softwaremill.sttp.client3" %% "core" % V.sttp3
    val sttp3Circe = "com.softwaremill.sttp.client3" %% "circe" % V.sttp3
    val circeGeneric = "io.circe" %% "circe-generic" % V.circe

    val catsEffect = "org.typelevel" %% "cats-effect" % V.cats

    val smlTagging = "com.softwaremill.common" %% "tagging" % V.smlCommon

    val scalaTest = "org.scalatest" %% "scalatest" % V.scalaTest

    val logback = "ch.qos.logback" % "logback-classic" % V.logback
    val slf4j = "org.slf4j" % "slf4j-api" % V.slf4j
    val catsLogging = "org.typelevel" %% "log4cats-slf4j" % V.catslogging

    val grpcNetty = "io.grpc" % "grpc-netty-shaded" % V.grpcNetty
    val grpcServices = "io.grpc" % "grpc-services" % V.grpcServices
  }
}
