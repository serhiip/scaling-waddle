package car.drivernotifier.grpc

import cats.effect.kernel.Resource
import cats.effect.IO
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import java.net.{InetSocketAddress, InetAddress}
import fs2.grpc.syntax.all._
import cats.effect.kernel.Sync
import io.grpc.protobuf.services.ProtoReflectionService

trait Server[F[_]] {
  def run(services: io.grpc.ServerServiceDefinition*): Resource[F, io.grpc.Server]
}

object Server {
  def impl[F[_]: Sync](address: String, port: Int): Server[F] = new Server[F] {
    override def run(services: io.grpc.ServerServiceDefinition*): Resource[F, io.grpc.Server] = {
      val builder = NettyServerBuilder.forAddress(new InetSocketAddress(InetAddress.getByName(address), port))
      services.foldLeft(builder)(_ addService _)
      builder.addService(ProtoReflectionService.newInstance())
      builder.resource[F]
    }
  }
}
