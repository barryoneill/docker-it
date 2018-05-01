package com.hbc.dockerit.util

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.{CreateStreamResult, GetRecordsRequest, GetShardIteratorRequest, PutRecordsRequest, PutRecordsRequestEntry, PutRecordsResult, Record, ShardIteratorType}

import scala.collection.JavaConverters._

/**
  * A wrapper for the kineis client, providing a bunch of convenience methods to save clutter in tests/matchers.
  */
case class KinesisUtil(client: AmazonKinesis) extends CirceSupport {

  def createStream(streamName: String): CreateStreamResult = client.createStream(streamName, 1)

  def getRecords[T](streamName: String)(implicit decoder: io.circe.Decoder[T]): Seq[T] = {

    val iteratorResp = client.getShardIterator(new GetShardIteratorRequest()
      .withStreamName(streamName)
      .withShardId("0")
      .withShardIteratorType(ShardIteratorType.TRIM_HORIZON))

    client.getRecords(new GetRecordsRequest().withShardIterator(iteratorResp.getShardIterator)).getRecords.asScala
          .map(r => new String(r.getData.array(), "UTF8"))
          .map(decodeOrThrow[T])
  }

  def putRecords[T](streamName: String, records: Seq[T], shardKeyFunc: T => String)(implicit encoder: io.circe.Encoder[T]): PutRecordsResult = {

    val request = new PutRecordsRequest()
      .withStreamName(streamName)
      .withRecords(asJavaCollection(records.map(r => {
        new PutRecordsRequestEntry()
          .withPartitionKey(shardKeyFunc(r))
          .withData(ByteBuffer.wrap(encode(r).getBytes("UTF-8")))
      })))

    client.putRecords(request)

  }

}
