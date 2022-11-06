package car.drivernotifier

import cats.effect.{IO, IOApp, ExitCode}
import car.drivernotifier.protos.notification.GetCount
import cats.syntax.option._
import cats.syntax.flatMap._
import car.drivernotifier.protos.notification_service.NotificationServiceFs2Grpc
import io.grpc.Metadata

object DriverNotifier extends IOApp {
  def run(args: List[String]) =
    (grpc.Client.builder[IO]("127.0.0.1", 9999).notification) use {
      notification: NotificationServiceFs2Grpc[IO, Metadata] =>
        (notification.count(GetCount(store = "driver-notifications".some), new Metadata()) >>= IO.println) >> IO(
          ExitCode.Success
        )
    }
}
