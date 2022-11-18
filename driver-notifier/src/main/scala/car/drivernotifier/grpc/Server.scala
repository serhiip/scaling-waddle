package car.drivernotifier.grpc

import cats.effect.Async
import cats.effect.kernel.Resource
import fs2.Stream
import fs2.grpc.syntax.all._
import io.grpc.Metadata
import io.grpc.Metadata.AsciiMarshaller
import io.grpc.Metadata.Key
import io.grpc.ServerStreamTracer
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.protobuf.services.ProtoReflectionService

import java.net.InetAddress
import java.net.InetSocketAddress

trait Server[F[_]] {
  def run(services: io.grpc.ServerServiceDefinition*): Resource[F, io.grpc.Server]
  def runStream(services: io.grpc.ServerServiceDefinition*): Stream[F, io.grpc.Server]
}

object Server {
  def impl[F[_]: Async](address: String, port: Int): Server[F] = new Server[F] {

    override def run(services: io.grpc.ServerServiceDefinition*): Resource[F, io.grpc.Server] = {
      val builder = NettyServerBuilder
        .forAddress(new InetSocketAddress(InetAddress.getByName(address), port))
      services.foldLeft(builder)(_ addService _)
      builder.addService(ProtoReflectionService.newInstance())
      builder.resource[F]
    }

    override def runStream(services: io.grpc.ServerServiceDefinition*): Stream[F, io.grpc.Server] = {
      val builder = NettyServerBuilder
        .forAddress(new InetSocketAddress(InetAddress.getByName(address), port))
      services.foldLeft(builder)(_ addService _)
      builder.addService(ProtoReflectionService.newInstance())
      builder.addStreamTracerFactory(new ServerStreamTracer.Factory {
        override def newServerStreamTracer(methodName: String, metadata: Metadata): ServerStreamTracer =
          new ServerStreamTracer {
            override def serverCallStarted(callInfo: ServerStreamTracer.ServerCallInfo[_, _]): Unit =  {

              println(metadata)
              metadata.put[String](
                Key.of(
                  "traceId",
                  new AsciiMarshaller[String] {
                    override def toAsciiString(in: String): String     = in
                    override def parseAsciiString(out: String): String = out
                  }
                ),
                "cccccccccc"
              )
            }
          }
      })
      builder.stream[F]
    }
  }
}
