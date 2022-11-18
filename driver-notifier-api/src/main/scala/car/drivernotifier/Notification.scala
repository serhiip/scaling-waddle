package car.drivernotifier

import cats.effect.Async
import car.drivernotifier.grpc.Client
import car.drivernotifier.protos.notification.GetCount
import cats.syntax.option._
import cats.syntax.functor._
import cats.syntax.flatMap._
import cats.Functor
import io.grpc.Metadata
import car.drivernotifier.http.CountResult
import trace4cats.Trace
import cats.syntax.show._
import cats.syntax.option._

trait Notification[F[_]] {
  def count(): F[CountResult]
}

object Notification {
  private val host = "localhost"
  private val port = 9999

  def impl[F[_]: Async: Functor: Trace](): Notification[F] = new Notification[F] {
    override def count(): F[CountResult] = Client.builder[F]("localhost", 9999).notification use { service =>
      Trace[F].span("calling grpc service to get notification count") {
        for {
          _         <- Trace[F].putAll("host" -> host, "port" -> port)
          headers   <- Trace[F].headers
          headersMap = headers.values map { case (k, v) => k.toString -> v }
          result    <- service
                         .count(
                           GetCount(
                             store = "driver-notifications".some,
                             headers = headersMap
                           ),
                           new Metadata()
                         )
                         .map(res => CountResult(res.value))
        } yield result
      }
    }
  }
}
