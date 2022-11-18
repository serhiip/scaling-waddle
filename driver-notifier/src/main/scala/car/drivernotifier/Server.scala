package car.drivernotifier

import car.drivernotifier.grpc.Server
import car.drivernotifier.protos.notification_service.NotificationServiceFs2Grpc
import cats.Applicative
import cats.effect
import cats.effect.IO
import cats.effect.MonadCancelThrow
import cats.effect.kernel.Async
import cats.effect.kernel.Resource
import fs2.Stream
import io.grpc.ServerServiceDefinition
import trace4cats.EntryPoint
import trace4cats._
import trace4cats.fs2.TracedStream
import trace4cats.fs2.syntax.all._
import trace4cats.kernel.SpanSampler
import trace4cats.model.TraceHeaders
import trace4cats.model.TraceProcess
import trace4cats.stackdriver.StackdriverGrpcSpanCompleter

object ServerApp extends effect.IOApp.Simple {

  private val host = "127.0.0.1"
  private val port = 9999

  def traceEntryPoint[F[_]: Async](process: TraceProcess): Resource[F, EntryPoint[F]] =
    StackdriverGrpcSpanCompleter[F](process, projectId = "poetic-centaur-287518").map { completer =>
      EntryPoint[F](SpanSampler.always[F], completer)
    }

  def getHeaders[F[_]](stream: TracedStream[F, io.grpc.Server]): Stream[F, (TraceHeaders, io.grpc.Server)] =
    stream.traceHeaders.endTrace

  private def notificationService(entryPoint: EntryPoint[IO]): Resource[IO, ServerServiceDefinition] =
    NotificationServiceFs2Grpc.bindServiceResource[IO](NotificationService.default(entryPoint))

  def runServer(service: ServerServiceDefinition): IO[Nothing] =
    Server.impl[IO](host, port).run(service).evalMap(server => IO(server.start())).useForever

  def runServer2[F[_]: Async](service: ServerServiceDefinition): Stream[F, io.grpc.Server] = {
    val r: Stream[F, io.grpc.Server] =
      Server.impl[F](host, port).runStream(service).map(server => server.start())

    val l: Stream[F, io.grpc.Server] = Server.impl[F](host, port).runStream(service).map(server => server.start())
//    l.flatMap(server => server.)
    r
  }

  def inject[F[_]: Async](ep: EntryPoint[F], service: ServerServiceDefinition): TracedStream[F, io.grpc.Server] =
    runServer2[F](service).inject(ep, "this is injected root span", SpanKind.Producer)

  def continue[F[_]: MonadCancelThrow](
      ep: EntryPoint[F],
      stream: Stream[F, (TraceHeaders, io.grpc.Server)]
  ): TracedStream[F, Unit] =
    // inject the entry point and extract headers from the stream element
    stream
      .injectContinue(ep, "this is the root span in a new service", SpanKind.Consumer)(_._1)
      .evalMap("child span in new service", SpanKind.Consumer) { _ =>
        Applicative[F].unit
      }

  // override def run(args: List[String]): Resource[IO, effect.ExitCode] =
  //   for {
  //     ep      <- traceEntryPoint[IO](TraceProcess("Notifications GRPC server"))
  //     service <- notificationService
  //     res     <- Resource liftK {
  //                  // inject the entry point into an infinite stream, do some work,
  //                  // then export the trace context as message headers
  //                  val headersStream = getHeaders(
  //                    inject(ep, service)
  //                  )

  //                  // simulate going across service boundaries by using the message headers
  //                  val continuedStream = continue(ep, headersStream)

  //                  continuedStream.run.compile.drain
  //                }
  //   } yield effect.ExitCode.Success

  override def run: IO[Nothing] = {
    traceEntryPoint[IO](TraceProcess("Notifications GRPC server")) use { ep: EntryPoint[IO] =>
      notificationService(ep).use(runServer)
    }
  }
}
