package car.drivernotifier.api

import cats.effect._
import com.comcast.ip4s._
import org.http4s.ember.server._
import car.drivernotifier.http.ApiSpec
import cats.syntax.semigroupk._
import trace4cats.kernel.Span
import cats.data.Kleisli
import cats.effect.{Concurrent, IO, Resource, ResourceApp}
import org.http4s.implicits._
import trace4cats._
import trace4cats.stackdriver.StackdriverGrpcSpanCompleter
import trace4cats.jaeger.JaegerSpanCompleter
import scala.concurrent.duration._
import trace4cats.http4s.server.syntax._
import trace4cats.Trace
import trace4cats.log.LogSpanCompleter
import cats.syntax.functor._
import cats.Applicative
import car.drivernotifier.Notification

object DriverNotifierApi extends ResourceApp.Forever {

  type F[A] = IO[A]
  type G[A] = Kleisli[F, Span[F], A]

  def traceEntryPoint[F[_]: Async](process: TraceProcess): Resource[F, EntryPoint[F]] =
    StackdriverGrpcSpanCompleter[F](process, projectId = "poetic-centaur-287518").map { completer =>
      EntryPoint[F](SpanSampler.always[F], completer)
    }

  def routes[F[_]: Async: Trace] = {
    val notificationComponent = Notification.impl[F]

    ApiSpec.getCountRoute[F](notification = notificationComponent) <+> ApiSpec.apiDocRoute[F]
  }

  def run(args: List[String]): Resource[F, Unit] =
    for {
      ep <- traceEntryPoint[F](TraceProcess("Driver Notification"))
      _  <- EmberServerBuilder
              .default[F]
              .withHost(ipv4"0.0.0.0")
              .withPort(port"8080")
              .withHttpApp(
                routes[G].inject(ep).orNotFound
              )
              .build
    } yield ()
}
