package car.drivernotifier

import car.drivernotifier.protos.notification._
import car.drivernotifier.protos.notification_service.NotificationServiceFs2Grpc
import cats.Applicative
import cats.data.Kleisli
import cats.effect.MonadCancelThrow
import cats.syntax.applicative._
import io.grpc.Metadata
import trace4cats.EntryPoint
import trace4cats.Span
import trace4cats.Trace
import trace4cats.model.TraceHeaders

object NotificationService {

  def default[F[_]: Applicative: MonadCancelThrow, A <: Metadata](ep: EntryPoint[F]): NotificationServiceFs2Grpc[F, A] =
    new NotificationServiceFs2Grpc[F, A] {

      type Traced[B] = Kleisli[F, Span[F], B]

      override def count(in: GetCount, ctx: A): F[Count] = {
        ep.continueOrElseRoot("GRPC Notification Service", TraceHeaders of in.headers).use { root =>
          runCount[Traced](in.store).run(root)
        }
      }

      private def runCount[R[_]: Trace: Applicative](store: Option[String]): R[Count] = {
        Trace[R].span("Query Kafka to get count of notifications") {
          Count(store = store, value = 22).pure[R]
        }
      }
    }
}
