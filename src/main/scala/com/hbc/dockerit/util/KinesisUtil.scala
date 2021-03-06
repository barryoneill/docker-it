package com.hbc.dockerit.util

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.{
  CreateStreamResult,
  GetRecordsRequest,
  GetShardIteratorRequest,
  PutRecordsRequest,
  PutRecordsRequestEntry,
  PutRecordsResult,
  ShardIteratorType
}

import scala.collection.JavaConverters._

/**
  * A wrapper for the kineis client, providing a bunch of convenience methods to save clutter in tests/matchers.
  */
case class KinesisUtil(client: AmazonKinesis) extends CirceSupport {

  def createStream(streamName: String, numShards: Int = 1): CreateStreamResult = {
    val createStreamResult = client.createStream(streamName, numShards)

    /* TODO: investigate: The createStream call will return but it appears the localstack
       stream isn't immediately available.  If you directly followup with, for example, a
       putRecords call, LS will 502 with "KeyError: 'Records'[\n]", and whatever proxying
       code is in use will hang for approx 20 seconds.  Sleeping, even 1 second, seems to
       mitigate the problem. (╯°□°）╯︵ ┻━┻ */
    Thread.sleep(1000)

    createStreamResult
  }

  def getRecordsJSON[T](
    streamName: String
  )(implicit decoder: io.circe.Decoder[T]): Seq[(String, T)] =
    getRecords(streamName).map { case (key, event) => key -> decodeOrThrow[T](event) }

  def getRecords(streamName: String): Seq[(String, String)] = {

    val iteratorResp = client.getShardIterator(
      new GetShardIteratorRequest()
        .withStreamName(streamName)
        .withShardId("0")
        .withShardIteratorType(ShardIteratorType.TRIM_HORIZON)
    )

    client
      .getRecords(new GetRecordsRequest().withShardIterator(iteratorResp.getShardIterator))
      .getRecords
      .asScala
      .map(r => r.getPartitionKey -> new String(r.getData.array(), "UTF8"))
  }

  def putRecordsJSON[T](streamName: String, records: Seq[T], shardKeyFunc: T => String)(
    implicit encoder: io.circe.Encoder[T]
  ): PutRecordsResult =
    putRecords(streamName, records.map(r => (shardKeyFunc(r), encode(r))))

  def putRecords(streamName: String, records: Seq[(String, String)]): PutRecordsResult = {

    val request = new PutRecordsRequest()
      .withStreamName(streamName)
      .withRecords(asJavaCollection(records.map {
        case (k, v) =>
          new PutRecordsRequestEntry()
            .withPartitionKey(k)
            .withData(ByteBuffer.wrap(v.getBytes("UTF-8")))
      }))

    client.putRecords(request)

  }

}
