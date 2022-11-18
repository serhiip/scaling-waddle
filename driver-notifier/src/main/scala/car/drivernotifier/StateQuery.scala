package car.drivernotifier

import cats.effect.kernel.Sync
import cats.syntax.applicative._
import cats.syntax.flatMap._
import cats.syntax.functor._
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.KafkaStreams
import org.apache.kafka.streams.StoreQueryParameters
import org.apache.kafka.streams.state.QueryableStoreTypes
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax._

trait StateQuery[F[_], K, V] {
  def getState(key: K, store: Store): Unit
  def count(store: Store): F[Long]
}

object StateQuery {
  def apply[F[_], K, V](implicit ev: StateQuery[F, K, V]): StateQuery[F, K, V] = ev

  def default[F[_]: Sync: Logger, K: Serde, V](streams: KafkaStreams): StateQuery[F, K, V] =
    new StateQuery[F, K, V] {
      override def count(s: Store): F[Long] = for {
        _             <- info"getting info from ${s.name}"
        val parameters = StoreQueryParameters.fromNameAndType(s.name, QueryableStoreTypes.keyValueStore())
        _             <- debug"query parameters are $parameters"
        store         <- streams.store(parameters).pure
        count         <- store.approximateNumEntries().pure
        _             <- info"there are $count entries in the store ${s.name}"
      } yield count

      override def getState(key: K, s: Store): Unit = for {
        _             <- info"getting key $key from ${s.name}"
        // keyMeta       <- {
        //   streams.queryMetadataForKey(
        //     store.name,
        //     key,
        //     implicitly[Serde[K]].serializer()
        //   )
        // }.pure
        val parameters = StoreQueryParameters.fromNameAndType(s.name, QueryableStoreTypes.keyValueStore())
        _             <- debug"query parameters are $parameters"
        store         <- streams.store(parameters).pure
        count         <- store.approximateNumEntries().pure
        _             <- info"there are $count entries in the store ${s.name}"
      } yield ()
    }
}
