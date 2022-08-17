package car.consumer

import car.avro._
import car.domain._
import cats.effect.{ExitCode, IO, IOApp, Resource}
import cats.implicits._
import com.sksamuel.avro4s.RecordFormat
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.clients.consumer.ConsumerConfig._
import org.apache.kafka.clients.consumer.KafkaConsumer

import java.time.Duration
import scala.jdk.CollectionConverters._

object CarDataConsumer extends IOApp {

  val props: Map[String, Object] = Map(
    GROUP_ID_CONFIG -> "car-metrics-consumer",
    BOOTSTRAP_SERVERS_CONFIG -> List(sys.env("KAFKA_HOST"), sys.env("KAFKA_PORT")).mkString(":"),
    KEY_DESERIALIZER_CLASS_CONFIG -> classOf[KafkaAvroDeserializer],
    VALUE_DESERIALIZER_CLASS_CONFIG -> classOf[KafkaAvroDeserializer],
    SCHEMA_REGISTRY_URL_CONFIG -> s"http://${sys.env("SCHEMA_REGISTRY_HOST")}:${sys.env("SCHEMA_REGISTRY_PORT")}"
  )

  override def run(args: List[String]): IO[ExitCode] =
    pollForever[CarId, DriverNotification]("driver-notification").as(ExitCode.Success)

  private def pollForever[K, V](topic: String)(implicit krf: RecordFormat[K], vrf: RecordFormat[V]): IO[Nothing] =
    Resource
      .make(IO {
        val consumer = new KafkaConsumer[IndexedRecord, IndexedRecord](CarDataConsumer.props.asJava)
        consumer.subscribe(Seq(topic).asJava)
        consumer
      })(c => IO(println(s"[$topic] closing consumer...")) *> IO(c.close()))
      .use { consumer =>
        val consume: IO[Unit] = for {
          records <- IO(consumer.poll(Duration.ofSeconds(5)).asScala.toSeq)
          keyValue = records.map { r => (krf.from(r.key()), vrf.from(r.value())) }
          _ <- keyValue.traverse { case (k, v) => IO(println(s"[$topic] $k => $v")) }
        } yield ()
        consume.foreverM
      }
}
