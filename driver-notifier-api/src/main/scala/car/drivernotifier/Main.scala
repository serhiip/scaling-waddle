package car.drivernotifier

import cats.effect._
import com.comcast.ip4s._
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._
import car.drivernotifier.http.ApiSpec
import cats.syntax.semigroupk._

object Main extends IOApp {

  val routes = ApiSpec.getCountRoute[IO] <+> ApiSpec.apiDocRoute[IO]

  def run(args: List[String]): IO[ExitCode] =
    EmberServerBuilder
      .default[IO]
      .withHost(ipv4"0.0.0.0")
      .withPort(port"8080")
      .withHttpApp(routes.orNotFound)
      .build
      .use(_ => IO.never)
      .as(ExitCode.Success)
}
