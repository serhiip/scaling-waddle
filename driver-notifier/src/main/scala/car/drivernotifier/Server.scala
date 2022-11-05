package car.drivernotifier

import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import cats.effect.IOApp
import cats.effect.kernel.Resource
import cats.effect.IO
import io.grpc.ServerServiceDefinition
import car.drivernotifier.protos.notification_service.NotificationServiceFs2Grpc
import car.drivernotifier.grpc.Server
import io.grpc.BindableService

object ServerApp extends IOApp {
  private val notificationService: Resource[IO, ServerServiceDefinition] =
    NotificationServiceFs2Grpc.bindServiceResource[IO](NotificationService.default)

  def runServer(service: ServerServiceDefinition): IO[Nothing] =
    Server.impl[IO]("127.0.0.1", 9999).run(service).evalMap(server => IO(server.start())).useForever

  override def run(args: List[String]): IO[Nothing] = notificationService.use(runServer)
}
