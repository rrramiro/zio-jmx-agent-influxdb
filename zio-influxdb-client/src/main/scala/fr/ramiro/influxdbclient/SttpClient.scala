package fr.ramiro.influxdbclient

import cats.Monoid
import com.softwaremill.sttp._
import com.softwaremill.sttp.asynchttpclient.zio.AsyncHttpClientZioBackend
import org.asynchttpclient.DefaultAsyncHttpClientConfig
import zio.{IO, Task}

import scala.collection.immutable

class SttpClient(config: SttpConfig) extends AutoCloseable {

  private lazy val protocol = if (config.https) "https" else "http"
  private var connectionClosed = false

  private implicit lazy val backend: SttpBackend[Task, Nothing] = {
    AsyncHttpClientZioBackend.usingConfig(
      new DefaultAsyncHttpClientConfig.Builder()
        .setMaxRequestRetry(1)
        .setConnectTimeout(config.connectTimeout)
        .setRequestTimeout(config.requestTimeout)
        .setUseInsecureTrustManager(config.acceptAnyCertificate)
        .build()
    )
  }

  def get(path: String, params: Parameter*) = makeRequest {
    sttp.get(baseUri(path, params: _*))
  }

  def post(path: String, content: String, params: Parameter*) =
    makeRequest {
      sttp.post(baseUri(path, params: _*)).body(content, "utf-8")
    }

  def close(): Unit = {
    connectionClosed = true
    backend.close()
  }

  def isClosed: Boolean = connectionClosed

  private def baseUri(url: String, params: Parameter*): Uri = {
    Uri(
      protocol,
      None,
      config.host,
      Some(config.port),
      immutable.Seq(url),
      immutable.Seq.empty,
      None
    ).params()
      .copy(queryFragments = Monoid.combineAll(params).toQueryFragments)
  }

  private def makeRequest(
      requestBuilder: Request[String, Nothing]
  ): Task[HttpResponse] = {
    if (isClosed) {
      IO.fail(HttpException("Connection is already closed"))
    } else {
      config.username
        .map(
          uName =>
            requestBuilder.auth.basic(uName, config.password.getOrElse(""))
        )
        .getOrElse(requestBuilder)
        .send()
        .foldM(
          throwable =>
            IO.fail(
              HttpException(
                "An error occurred during the request",
                -1,
                Some(throwable)
              )
            ), { response: Response[String] =>
            if (response.code >= 400)
              IO.fail(
                HttpException(
                  s"Server answered with error code ${response.code}. Message: ${response.body}",
                  response.code
                )
              )
            else
              IO.succeed(HttpResponse(response.code, response.body.merge))
          }
        )
    }
  }
}

case class HttpException(
    str: String,
    code: Int = -1,
    throwable: Option[Throwable] = None
) extends Exception(str, throwable.orNull)

case class HttpResponse(code: Int, content: String)
