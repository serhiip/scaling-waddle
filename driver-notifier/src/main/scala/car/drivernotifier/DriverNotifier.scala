package car.drivernotifier

import car.avro._
import car.domain._
import car.drivernotifier.AvroSerdes._
import cats.implicits._
import com.sksamuel.avro4s.BinaryFormat
import com.sksamuel.avro4s.kafka.GenericSerde
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig.{APPLICATION_ID_CONFIG, BOOTSTRAP_SERVERS_CONFIG}
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.{KGroupedStream, KTable, Materialized}
import org.apache.kafka.streams.state.KeyValueStore
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.apache.kafka.streams.StoreQueryParameters

import java.util.Properties
import org.apache
import java.time.Duration

import scala.jdk.CollectionConverters._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KeyQueryMetadata
import org.typelevel.log4cats.slf4j.Slf4jLogger
import org.typelevel.log4cats.syntax._
import cats.effect.kernel.Sync
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.ExitCode
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import org.typelevel.log4cats.SelfAwareStructuredLogger
import org.typelevel.log4cats._
import cats.syntax.flatMap._
import cats.syntax.monad._
import cats.effect.Temporal
import scala.concurrent.duration._
import cats.FlatMap
import cats.effect.kernel.Outcome

object DriverNotifier extends IOApp {

  implicit def logger[F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  def run(args: List[String]) = {
    val props = new Properties()
    props.put(APPLICATION_ID_CONFIG, "driver-notifier")
    props.put(BOOTSTRAP_SERVERS_CONFIG, List(sys.env("KAFKA_HOST"), sys.env("KAFKA_PORT")).mkString(":"))

    val storeName = "notifications-store"
    val store     = Store(storeName)

    for {
      topology <- Notifier.default[IO](storeName).getTopology
      streams  <- IO { new KafkaStreams(topology, props) }
      _        <- IO { streams.start() }
      _        <- IO {
                    Runtime.getRuntime.addShutdownHook(new Thread {
                      override def run() = streams.close()
                    })
                  }
      val query = StateQuery.default[IO, CarId, DriverNotification](streams)
      _        <- IO.asyncForIO.background(printCounts(store, query).foreverM) use { (_: IO[Outcome[IO, _, _]]) =>
                    IO.never onCancel { IO apply streams.close() }
                  }
    } yield ExitCode.Success
  }

  def printCounts[F[_]: Temporal, K: Serde, V](store: Store, query: StateQuery[F, K, V]): F[Unit] =
    query.count(store) *> Temporal[F].sleep(1.second)
}
