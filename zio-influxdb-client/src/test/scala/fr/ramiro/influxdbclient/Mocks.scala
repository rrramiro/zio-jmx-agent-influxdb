package fr.ramiro.influxdbclient

import zio.IO

object Mocks {
  class ExceptionThrowingHttpClient(config: SttpConfig)
      extends SttpClient(config) {

    override def post(url: String, content: String, params: Parameter*) =
      IO.fail(new HttpException(""))

    override def get(url: String, params: Parameter*) =
      IO.fail(new HttpException(""))
  }

  class ErrorReturningHttpClient(config: SttpConfig, errorCode: Int)
      extends SttpClient(config) {

    override def post(url: String, content: String, params: Parameter*) =
      IO.succeed(new HttpResponse(errorCode, "Error message"))

    override def get(url: String, params: Parameter*) =
      IO.succeed(new HttpResponse(errorCode, "Error message"))
  }

}
