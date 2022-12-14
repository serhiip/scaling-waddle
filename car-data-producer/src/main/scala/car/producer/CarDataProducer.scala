package car.producer

import car.avro._
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Resource
import cats.implicits._
import com.sksamuel.avro4s.RecordFormat
import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.generic.IndexedRecord
import org.apache.kafka.clients.producer.Callback
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.CLIENT_ID_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata

import scala.concurrent.Promise
import scala.concurrent.duration.DurationInt
import scala.jdk.CollectionConverters._

object CarDataProducer extends IOApp {

  private val props: Map[String, Object] = Map(
    CLIENT_ID_CONFIG -> "car-metrics-producer",
    BOOTSTRAP_SERVERS_CONFIG -> List(sys.env("KAFKA_HOST"), sys.env("KAFKA_PORT")).mkString(":"),
    KEY_SERIALIZER_CLASS_CONFIG -> classOf[KafkaAvroSerializer],
    VALUE_SERIALIZER_CLASS_CONFIG -> classOf[KafkaAvroSerializer],
    SCHEMA_REGISTRY_URL_CONFIG -> s"http://${sys.env("SCHEMA_REGISTRY_HOST")}:${sys.env("SCHEMA_REGISTRY_PORT")}"
  )

  override def run(args: List[String]): IO[ExitCode] =
    Resource
      .make(IO(new KafkaProducer[IndexedRecord, IndexedRecord](props.asJava)))(p => IO(println("closing producer...")) *> IO(p.close()))
      .use { producer =>
        Seq(
          IO(RandomData.carSpeed).flatMap(send(producer)("car-speed", _)).foreverM,
          IO(RandomData.carEngine).flatMap(send(producer)("car-engine", _)).foreverM,
          IO(RandomData.carLocation).flatMap(send(producer)("car-location", _)).foreverM,
          IO(RandomData.locationData).flatMap(send(producer)("location-data", _)).foreverM
        ).parSequence_.as(ExitCode.Success)
      }

  private def send[K, V](
      producer: KafkaProducer[IndexedRecord, IndexedRecord]
  )(topic: String, records: Seq[(K, V)])(implicit krf: RecordFormat[K], vrf: RecordFormat[V]): IO[Unit] =
    records.traverse {
      case (k, v) =>
        val p = Promise[Unit]()
        producer.send(
          new ProducerRecord[IndexedRecord, IndexedRecord](topic, krf.to(k), vrf.to(v)),
          new Callback {
            override def onCompletion(metadata: RecordMetadata, exception: Exception): Unit =
              Option(exception).map(p.failure).getOrElse(p.success(()))
          }
        )
        IO.fromFuture(IO(p.future)) *> IO(println(s"produced data to [$topic]")) *> IO.sleep(1.second)
    }.void
}
