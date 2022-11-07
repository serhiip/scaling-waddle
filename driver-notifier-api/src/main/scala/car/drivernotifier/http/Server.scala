package car.drivernotifier.http

import sttp.tapir._
import sttp.tapir.generic.auto._
import sttp.tapir.json.circe._
import io.circe.generic.auto._
import java.util.UUID
import sttp.tapir.EndpointIO.annotations._
import sttp.tapir.server.http4s.Http4sServerInterpreter
import cats.effect.IO
import org.http4s.HttpRoutes
import cats.syntax.either._
import sttp.tapir.swagger.bundle.SwaggerInterpreter
import cats.effect.kernel.Async
import cats.syntax.applicative._
import trace4cats.Trace

final case class CarId(id: Int)
final case class CountResult(value: Int)

object ApiSpec {
  val getCountSpec: PublicEndpoint[Unit, Unit, CountResult, Any] =
    endpoint.in("car" / "notification" / "count").out(jsonBody[CountResult])

  def getCountRoute[F[_]: Async: Trace]: HttpRoutes[F] =
    Http4sServerInterpreter[F]().toRoutes(
      getCountSpec.serverLogic(_ => Trace[F].span("entuhentu") { (CountResult apply 22).asRight[Unit].pure[F] })
    )

  def apiDocRoute[F[_]: Async]: HttpRoutes[F] = Http4sServerInterpreter[F]().toRoutes(
    SwaggerInterpreter().fromEndpoints[F](List apply getCountSpec, "Driver Notifier", "1.0")
  )
}
