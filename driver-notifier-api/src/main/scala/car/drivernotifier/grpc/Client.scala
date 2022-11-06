package car.drivernotifier.grpc

import cats.effect.Resource
import cats.effect.IO
import io.grpc.ManagedChannel
import cats.effect.kernel.Sync
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.grpc.netty.NegotiationType
import fs2.grpc.syntax.all._
import car.drivernotifier.protos.notification_service.NotificationServiceFs2Grpc
import cats.syntax.flatMap._
import cats.effect.kernel.Async
import io.grpc.Metadata

trait Client[F[_]] {
  def notification: Resource[F, NotificationServiceFs2Grpc[F, Metadata]]
}

object Client {
  def builder[F[_]: Async](host: String, port: Int): Client[F] = new Client[F] {
    def notification: Resource[F, NotificationServiceFs2Grpc[F, Metadata]] = for {
      ch  <- NettyChannelBuilder.forAddress(host, port).negotiationType(NegotiationType.PLAINTEXT).resource[F]
      res <- NotificationServiceFs2Grpc.stubResource[F](ch)
    } yield res
  }
}
