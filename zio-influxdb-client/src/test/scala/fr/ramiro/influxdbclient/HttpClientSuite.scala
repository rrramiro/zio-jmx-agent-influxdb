package fr.ramiro.influxdbclient

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import org.scalatest.BeforeAndAfter

class HttpClientSuite extends CustomTestSuite with BeforeAndAfter {

  var host = "localhost"
  var port = 64011
  var httpsPort = 64012
  var mockServer: WireMockServer = new WireMockServer(
    wireMockConfig()
      .port(port)
      .containerThreads(10)
      .jettyAcceptors(1)
      .httpsPort(httpsPort)
  )
  mockServer.start()
  WireMock.configureFor(host, port)

  before {
    WireMock.reset()
  }

  override def afterAll = {
    mockServer.shutdown()
    super.afterAll
  }

  ignore("Basic requests are received") {
    val path = "query"
    stubFor(
      get(urlEqualTo(s"/$path"))
        .willReturn(aResponse().withStatus(200).withBody(""))
    )

    withClient(SttpConfig(host, port)) { client =>
      for {
        result <- client.get(path)
      } yield {
        assert(result.code == 200)
      }
    }
  }

  ignore("Https requests are received") {
    val path = "query"
    stubFor(
      get(urlEqualTo(s"/$path"))
        .willReturn(aResponse().withStatus(200).withBody(""))
    )

    withClient(
      SttpConfig(host, httpsPort, true, None, None, acceptAnyCertificate = true)
    ) { client =>
      for {
        result <- client.get(path)
      } yield {
        assert(result.code == 200)
      }
    }
  }

  ignore("Error responses are handled correctly") {
    val path = "query"
    stubFor(
      get(urlEqualTo(s"/$path"))
        .willReturn(
          aResponse()
            .withStatus(500)
            .withBody("")
        )
    )

    withClient(SttpConfig(host, port)) { client =>
      for {
        result <- client.get(path).either
      } yield {
        result match {
          case Left(e: HttpException) => assert(e.code == 500)
          case x                      => fail(s"Unexpected: $x")
        }
      }
    }
  }

  ignore("Future fails on connection refused") {
    withClient(SttpConfig(host, port - 1, false, None, None)) { client =>
      for {
        result <- client.get("query").either
      } yield {
        result match {
          case Left(e: HttpException) =>
          case x                      => fail(s"Unexpected: $x")
        }
      }
    }
  }
//TODO
  ignore("Future fails if request takes too long") {
    val path = "query"
    stubFor(
      get(urlEqualTo(s"/$path"))
        .willReturn(
          aResponse()
            .withFixedDelay(500)
            .withStatus(200)
            .withBody("a")
        )
    )

    withClient(SttpConfig(host, port, false, None, None, requestTimeout = 50)) {
      client =>
        for {
          result <- client.get(path).either
        } yield {
          result match {
            case Left(e: HttpException) =>
            case x                      => fail(s"Unexpected: $x")
          }
        }
    }
  }

  ignore("Future fails if the connection takes too long to establish") {
    withClient(
      SttpConfig("192.0.2.1", port, false, None, None, connectTimeout = 50)
    ) { client =>
      for {
        result <- client.get("query").either
      } yield {
        result match {
          case Left(e: HttpException) =>
          case x                      => fail(s"Unexpected: $x")
        }
      }
    }
  }

  test("Using a closed connection to send a query returns an exception") {
    val client = new SttpClient(SttpConfig(host, port))
    client.close()
    val result = await(client.get("query").either)
    result match {
      case Left(e: HttpException) =>
      case x                      => fail(s"Unexpected: $x")
    }
  }
}
