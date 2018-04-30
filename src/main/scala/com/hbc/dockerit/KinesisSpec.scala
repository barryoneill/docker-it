package com.hbc.dockerit

import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.services.kinesis.model.{CreateStreamResult, GetRecordsRequest, GetShardIteratorRequest, Record, ShardIteratorType}
import com.amazonaws.services.kinesis.{AmazonKinesis, AmazonKinesisClientBuilder}

import scala.collection.JavaConverters._

trait KinesisSpec { dockerSupport: DockerSupport =>

  def buildKinesisClient: AmazonKinesis = AmazonKinesisClientBuilder.standard()
    .withCredentials(dockerSupport.localstackAWSCredProvider)
    .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(s"http://localhost:$getMappedKinesisPort/", "us-east-1"))
    .build()

  def kinesisCreateStream(streamName: String): CreateStreamResult = {
    println(s"Calling localstack to create stream: $streamName")
    buildKinesisClient.createStream(streamName, 1)
  }

  def kinesisGetRecords(streamName: String): Seq[Record] = {

    println(s"Calling localstack to query stream $streamName")

    val client = buildKinesisClient

    val iteratorResp = client.getShardIterator(new GetShardIteratorRequest()
                  .withStreamName(streamName)
                  .withShardId("0")
                  .withShardIteratorType(ShardIteratorType.TRIM_HORIZON))

    client.getRecords(new GetRecordsRequest().withShardIterator(iteratorResp.getShardIterator))
      .getRecords.asScala

  }

}
