package car.drivernotifier

import car.avro._
import car.domain._
import car.drivernotifier.AvroSerdes._
import cats.Applicative
import cats.implicits._
import com.sksamuel.avro4s.BinaryFormat
import com.sksamuel.avro4s.kafka.GenericSerde
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.Topology
import org.apache.kafka.streams.scala.ImplicitConversions._
import org.apache.kafka.streams.scala._
import org.apache.kafka.streams.scala.kstream.KGroupedStream
import org.apache.kafka.streams.scala.kstream.KTable
import org.apache.kafka.streams.scala.kstream.Materialized
import org.apache.kafka.streams.state.KeyValueStore

import java.time.Duration

trait Notifier[F[_]] {
  def getTopology: F[Topology]
}

object Notifier {
  def apply[F[_]](implicit ev: Notifier[F]): Notifier[F] = ev

  def default[F[_]: Applicative](storeName: String): Notifier[F] = new Notifier[F] {

    import DriverNotifierData._

    def getTopology: F[Topology] = {
      val builder: StreamsBuilder = new StreamsBuilder

      val carSpeed: KGroupedStream[CarId, CarSpeed]       = builder.stream[CarId, CarSpeed]("car-speed").groupByKey
      val carEngine: KGroupedStream[CarId, CarEngine]     = builder.stream[CarId, CarEngine]("car-engine").groupByKey
      val carLocation: KGroupedStream[CarId, CarLocation] =
        builder.stream[CarId, CarLocation]("car-location").groupByKey
      val locationData: KTable[LocationId, LocationData]  = builder.table[LocationId, LocationData]("location-data")

      implicit val carDataSerde: GenericSerde[CarData] = new GenericSerde[CarData](BinaryFormat)

      val carData: KTable[CarId, CarData] = carSpeed
        .cogroup[CarData]({ case (_, speed, agg) => agg.copy(speed = speed.some) })
        .cogroup[CarEngine](carEngine, { case (_, engine, agg) => agg.copy(engine = engine.some) })
        .cogroup[CarLocation](carLocation, { case (_, location, agg) => agg.copy(location = location.some) })
        .aggregate(CarData.empty)

      implicit val carAndLocationDataSerde: GenericSerde[CarAndLocationData] =
        new GenericSerde[CarAndLocationData](BinaryFormat)

      val mat = Materialized
        .as[CarId, CarAndLocationData, KeyValueStore[Bytes, Array[Byte]]](storeName)
        .withRetention(Duration.ofDays(2L))

      val carAndLocationData: KTable[CarId, CarAndLocationData] = carData
        .filter({ case (_, carData) => carData.location.isDefined })
        .join[CarAndLocationData, LocationId, LocationData](
          locationData,
          keyExtractor = (carData: CarData) => carData.location.get.locationId,
          joiner = (carData: CarData, locationData: LocationData) => CarAndLocationData(carData, locationData),
          materialized = mat
        )

      carAndLocationData.toStream.flatMapValues(DriverNotifications(_)).to("driver-notification")

      builder.build().pure[F]
    }
  }
}
