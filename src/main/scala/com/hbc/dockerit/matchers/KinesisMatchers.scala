package com.hbc.dockerit.matchers

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.kinesis.model.{CreateStreamResult, GetRecordsRequest, GetShardIteratorRequest, Record, ShardIteratorType}
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}
import com.hbc.dockerit.containers.LocalstackContainer
import org.scalatest.Matchers

import scala.collection.JavaConverters._

trait KinesisMatchers extends Matchers {
  container: LocalstackContainer =>

  lazy val kinesis: AmazonKinesis = AmazonKinesisClientBuilder.standard()
    .withCredentials(container.dummyAWSCreds)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$portKinesis/", "us-east-1"))
    .build()

  def kinesisCreateStream(streamName: String): CreateStreamResult = {
    println(s"Calling localstack to create stream: $streamName")
    kinesis.createStream(streamName, 1)
  }

  def kinesisGetRecords(streamName: String): Seq[Record] = {

    println(s"Calling localstack to query stream $streamName")

    val iteratorResp = kinesis.getShardIterator(new GetShardIteratorRequest()
      .withStreamName(streamName)
      .withShardId("0")
      .withShardIteratorType(ShardIteratorType.TRIM_HORIZON))

    kinesis.getRecords(new GetRecordsRequest().withShardIterator(iteratorResp.getShardIterator)).getRecords.asScala
  }


}