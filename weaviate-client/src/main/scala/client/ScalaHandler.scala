package client

import java.util.Map;

import com.amazonaws.services.lambda.runtime.{Context, RequestHandler}
import com.amazonaws.services.lambda.runtime.events.{
    APIGatewayV2HTTPEvent,
    APIGatewayV2HTTPResponse
}

import sttp.client3.{SimpleHttpClient, UriContext, basicRequest}

import ml.combust.mleap.runtime._
import ml.combust.mleap.core.types._
import ml.combust.mleap.runtime.MleapSupport._
import ml.combust.mleap.runtime.frame.{DefaultLeapFrame, Row}
import ml.combust.bundle.BundleFile

import resource._
import scala.collection.JavaConversions;
import scala.util.{Try, Success, Failure}

class ScalaHandler
    extends RequestHandler[APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse] {

    val client = SimpleHttpClient()

    val bundle =
        (for (
          bundle <- managed(
            BundleFile("file:/opt")
          )
        ) yield {
            bundle.loadMleapBundle().get
        }).tried.get

    override def handleRequest(
        event: APIGatewayV2HTTPEvent,
        context: Context
    ): APIGatewayV2HTTPResponse = {
        val result = queryDatabase(event)

        return APIGatewayV2HTTPResponse
            .builder()
            .withHeaders(
              Map.of(
                "Access-Control-Allow-Headers",
                "*",
                "Access-Control-Allow-Origin",
                "*"
              )
            )
            .withStatusCode(result match {
                case Success(graphQlResult) => 200
                case Failure(reason)        => 500
            })
            .withBody(result match {
                case Success(graphQlResult) => graphQlResult
                case Failure(reason)        => reason.toString()
            })
            .build()
    }

    def queryDatabase(event: APIGatewayV2HTTPEvent): Try[String] = Try {
        val queryParams =
            event.getQueryStringParameters

        var query = None: Option[String]
        var distance = None: Option[Float]

        try {
            query = Some(queryParams.get("query"))
            distance = Some(queryParams.get("distance").toFloat)
        } catch {
            case e: Exception =>
                throw new Exception("Invalid query parameters.")
        }

        val vector = getStringVector(query.get)
        val graphQlQuery = buildGraphQlQuery(vector, distance.get)

        val response = client
            .send(
              basicRequest
                  .contentType("application/json")
                  .post(uri"${sys.env.get("WEAVIATE_ENDPOINT").get}")
                  .body(graphQlQuery)
            )

        response.body.right.get
    }

    def getStringVector(query: String): String = {
        val schema: StructType = StructType(
          StructField("input", ScalarType.String)
        ).get

        val dataset = Seq(Row(query))

        val leapFrame = DefaultLeapFrame(schema, dataset)

        val bundle =
            (for (
              bundle <- managed(
                BundleFile("file:/opt")
              )
            ) yield {
                bundle.loadMleapBundle().get
            }).tried.get

        bundle.root
            .transform(leapFrame)
            .get
            .dataset
            .head
            .getTensor(4)
            .toDense
            .toArray
            .mkString("[", ", ", "]")
    }

    def buildGraphQlQuery(vector: String, distance: Float): String = {
        val query = s"""
            |{
              |Get{
              |  Domain(
              |    limit: 500
              |    nearVector: {
              |      vector: ${vector}
              |      distance: ${distance}
              |    }
              |  ){
              |    name
              |    _additional {
              |      distance
              |    }
              |  }
              |}
            |}
            """.stripMargin('|')

        queryToJson(query)
    }

    def queryToJson(query: String): String = {
        val jsonFriendlyQuery =
            query.replace("\n", "").replace(""""""", """\"""")
        s"""{"query": "$jsonFriendlyQuery"}"""
    }

}
