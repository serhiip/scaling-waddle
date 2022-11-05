package car.drivernotifier

import car.drivernotifier.protos.notification_service.NotificationServiceFs2Grpc
import car.drivernotifier.protos.notification._
import cats.Applicative
import cats.syntax.applicative._

object NotificationService {
  def default[F[_]: Applicative, A](): NotificationServiceFs2Grpc[F, A] = new NotificationServiceFs2Grpc[F, A] {

    override def count(in: GetCount, ctx: A) = Count(store = in.store, value = 22).pure

  }
}
