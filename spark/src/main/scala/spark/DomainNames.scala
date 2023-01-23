package com.nameringers.spark

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions.{lit, col, concat, regexp_extract}
import org.apache.spark.ml.{Pipeline, PipelineStage, PipelineModel}
import org.apache.spark.ml.feature.{RegexTokenizer, NGram, HashingTF, IDF}
import org.apache.spark.ml.linalg.SparseVector
import org.apache.spark.ml.bundle.SparkBundleContext

import ml.combust.mleap.spark.SparkSupport._
import ml.bundle.BundleOuterClass$TensorDimension
import ml.combust.bundle.BundleFile
import ml.combust.bundle.serializer.SerializationFormat

import resource._
import scala.sys.process._

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}

import technology.semi.weaviate.client.v1.schema.model.{Property, WeaviateClass}

import java.io.File

case class Domain(name: String, gtld: String, features: Array[Float])

object DomainNames {

    def main(args: Array[String]): Unit = {

        val Array(zoneFileUri, bucketName, weaviateHost) = args

        val spark = SparkSession
            .builder()
            .appName("nameringers")
            .getOrCreate()

        val df = spark.read
            .textFile(zoneFileUri)
            .toDF()
            .withColumnRenamed("value", "name")
            .withColumn(
              "gtld",
              regexp_extract(col("name"), "\\.([a-z]+)$", 1)
            )
            .withColumn("sld", regexp_extract(col("name"), "^([^.]+)", 1))
            .withColumn("input", concat(lit(" "), col("sld"), lit(" ")))

        val tokenizer = new RegexTokenizer()
            .setInputCol("input")
            .setOutputCol("tokens")
            .setPattern("")

        val ngram =
            new NGram().setN(3).setInputCol("tokens").setOutputCol("ngrams")

        val hashingTF = new HashingTF()
            .setInputCol("ngrams")
            .setOutputCol("rawFeatures")
            .setNumFeatures(2048)

        val idf = new IDF().setInputCol("rawFeatures").setOutputCol("features")

        val pipeline = new Pipeline()
            .setStages(Array(tokenizer, ngram, hashingTF, idf))

        val model = pipeline.fit(df)

        val transformedData = model.transform(df)

        implicit val context =
            SparkBundleContext().withDataset(transformedData)

        for (
          bundle <- managed(
            BundleFile("jar:file:/tmp/tfidf.zip")
          )
        ) {
            model.writeBundle
                .format(SerializationFormat.Json)
                .save(bundle)(context)
                .get
        }

        val s3Client: AmazonS3 = AmazonS3ClientBuilder.defaultClient()

        s3Client.putObject(
          bucketName,
          "models/tfidf/bundle.zip",
          new java.io.File("/tmp/tfidf.zip")
        )

        import spark.implicits._

        transformedData
            .select("name", "gtld", "features")
            .map(row => {
                val vector: SparseVector = row.getAs("features")
                (Domain(
                  row.getAs("name"),
                  row.getAs("gtld"),
                  vector.toArray.map(x => x.toFloat)
                ))
            })
            .write
            .format("io.weaviate.spark.Weaviate")
            .option("scheme", "http")
            .option("host", weaviateHost)
            .option("batchSize", 200)
            .option("vector", "features")
            .option("className", "Domain")
            .mode("append")
            .save()

        spark.stop()
    }

}
