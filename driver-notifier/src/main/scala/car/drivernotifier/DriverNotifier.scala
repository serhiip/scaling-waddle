package car.drivernotifier

import car.avro._
import car.domain._
import car.drivernotifier.AvroSerdes._
import cats.effect.ExitCode
import cats.effect.IO
import cats.effect.IOApp
import cats.effect.Temporal
import cats.effect.kernel.Outcome
import cats.effect.kernel.Sync
import cats.implicits._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StreamsConfig.APPLICATION_ID_CONFIG
import org.apache.kafka.streams.StreamsConfig.BOOTSTRAP_SERVERS_CONFIG
import org.typelevel.log4cats._
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.Properties
import scala.concurrent.duration._

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
      streams  <- IO apply new KafkaStreams(topology, props)
      _        <- IO blocking streams.start()
      _        <- IO blocking {
                    Runtime.getRuntime.addShutdownHook(new Thread {
                      override def run() = streams.close()
                    })
                  }
      query     = StateQuery.default[IO, CarId, DriverNotification](streams)
      _        <- IO.asyncForIO.background(printCounts(store, query).foreverM) use { (_: IO[Outcome[IO, _, _]]) =>
                    IO.never onCancel { IO blocking streams.close() }
                  }
    } yield ExitCode.Success
  }

  def printCounts[F[_]: Temporal, K: Serde, V](store: Store, query: StateQuery[F, K, V]): F[Unit] =
    query.count(store) *> Temporal[F].sleep(1.second)
}
