package com.hbc.dockerit.matchers

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisAsyncClientBuilder}
import com.hbc.dockerit.containers.LocalStackContainer
import com.hbc.dockerit.util.{CirceSupport, KinesisUtil}
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

trait KinesisMatchers extends Matchers with CirceSupport {
  container: LocalStackContainer =>

  lazy val kinesis: AmazonKinesis = AmazonKinesisAsyncClientBuilder.standard()
    .withCredentials(container.dummyAWSCreds)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$portKinesis/", "us-east-1"))
    .build()

    def havePendingEvents[T](streamName: String, expectedEvents: Seq[T])
                                (implicit decoder: io.circe.Decoder[T],
                                 encoder: io.circe.Encoder[T]) = Matcher { (kinesis: AmazonKinesis) =>

      val actualEvents = KinesisUtil(kinesis).getRecords[T](streamName)

      MatchResult(actualEvents.size == expectedEvents.size && actualEvents.toSet == expectedEvents.toSet,
        s"""Expected events $expectedEvents but got $actualEvents""",
        s"""Found only $actualEvents"""
      )
  }

}