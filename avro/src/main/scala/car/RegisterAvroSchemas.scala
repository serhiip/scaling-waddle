package car.avro

import io.circe.generic.auto._
import sttp.client3.HttpURLConnectionBackend
import sttp.client3._
import sttp.client3.circe._

object RegisterAvroSchemas extends App {

  case class RegisterSchemaRequest(schema: String)

  val backend = HttpURLConnectionBackend()
  val srHost = sys.env("SCHEMA_REGISTRY_HOST")
  val srPort = sys.env("SCHEMA_REGISTRY_PORT")

  Seq(
    ("car-speed-key", RegisterSchemaRequest(carIdSchema.toString())),
    ("car-speed-value", RegisterSchemaRequest(carSpeedSchema.toString())),
    //
    ("car-engine-key", RegisterSchemaRequest(carIdSchema.toString())),
    ("car-engine-value", RegisterSchemaRequest(carEngineSchema.toString())),
    //
    ("car-location-key", RegisterSchemaRequest(carIdSchema.toString())),
    ("car-location-value", RegisterSchemaRequest(carLocationSchema.toString())),
    //
    ("location-data-key", RegisterSchemaRequest(locationIdSchema.toString())),
    ("location-data-value", RegisterSchemaRequest(locationDataSchema.toString())),
    //
    ("driver-notification-key", RegisterSchemaRequest(carIdSchema.toString())),
    ("driver-notification-value", RegisterSchemaRequest(driverNotificationSchema.toString()))
  ).map { case (subject, schema) =>
    subject -> basicRequest
      .post(uri"http://${srHost}:${srPort}/subjects/$subject/versions")
      .contentType("application/vnd.schemaregistry.v1+json")
      .body(schema)
      .send(backend)
      .code
  } foreach { case (subject, statusCode) => println(s"Register schema $subject, response code: $statusCode") }
}
