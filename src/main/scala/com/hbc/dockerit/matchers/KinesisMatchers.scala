package com.hbc.dockerit.matchers

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.hbc.dockerit.containers.LocalstackContainer
import com.hbc.dockerit.util.{CirceSupport, KinesisUtil}
import com.twitter.finagle.Redis.Client
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

trait KinesisMatchers extends Matchers with CirceSupport {
  container: LocalstackContainer =>

  lazy val kinesis: AmazonKinesis = AmazonKinesisClientBuilder.standard()
    .withCredentials(container.dummyAWSCreds)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$portKinesis/", "us-east-1"))
    .build()

    def havePendingEvents[T](streamName: String, expectedEvents: Seq[T])
                                (implicit decoder: io.circe.Decoder[T],
                                 encoder: io.circe.Encoder[T]) = Matcher { (kinesis: AmazonKinesis) =>

      val events = KinesisUtil(kinesis).getRecords[T](streamName)

      println(events)

      MatchResult(true, "", "")
  }

  //
  //  def haveEncodedValueOnGet[T](key: String, expectedObj: T)
  //                              (implicit decoder: io.circe.Decoder[T],
  //                               encoder: io.circe.Encoder[T]) = Matcher { (c: Client) =>
  //
  //    val expectedJSON = encode[T](expectedObj)
  //
  //    RedisUtil(c).get(key) match {
  //
  //      case Some(actualJSON) =>
  //
  //        val actualObj = decodeOrThrow[T](actualJSON)
  //
  //        MatchResult(
  //          actualObj == expectedObj,
  //          s"""Expected value "$expectedJSON" but got "$actualJSON" for key $key""",
  //          s"""Values match: $expectedObj for key $key""")
  //
  //
  //      case None => MatchResult(matches = false, s"""No value found for $key""", s"""No value found for $key""")
  //
  //    }
  //  }

}