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
        val queryParams =
            event.getQueryStringParameters

        val query = queryParams.get("query")
        val distance = queryParams.get("distance").toFloat

        val vector = getStringVector(query)

        val graphQlQuery = buildGraphQlQuery(vector, distance)

        val response = client
            .send(
              basicRequest
                  .contentType("application/json")
                  .post(uri"${sys.env.get("WEAVIATE_ENDPOINT").get}")
                  .body(graphQlQuery)
            )

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
            .withStatusCode(200)
            .withBody(response.body.right.get)
            .build()
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
