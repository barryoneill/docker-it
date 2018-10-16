package com.hbc.dockerit.util
import java.time.{Duration => JDuration}
import java.util.Collections.singletonList
import java.util.concurrent.TimeUnit.SECONDS

import com.hbc.dockerit.containers.Kafka
import org.apache.kafka.clients.admin.NewTopic
import org.apache.kafka.clients.producer.{ProducerRecord, RecordMetadata}

import scala.collection.JavaConverters._

/**
  * A wrapper for the redis client, providing a bunch of synchronous convenience
  * methods to save clutter in tests/matchers.
  */
case class KafkaUtil(kafka: Kafka) extends CirceSupport {

  val DefaultWaitSecs = 5

  def listTopics(): Seq[String] = kafka.withAdminClient { client =>
    client
      .listTopics()
      .names()
      .get(DefaultWaitSecs, SECONDS)
      .asScala
      .toSeq
  }

  def createTopics(topicNames: String*): Unit = kafka.withAdminClient { client =>
    client
      .createTopics(topicNames.map(new NewTopic(_, 1, 1)).asJava)
      .all()
      .get(DefaultWaitSecs * topicNames.size, SECONDS)
  }

  def deleteTopics(topicNames: String*): Unit = kafka.withAdminClient { client =>
    client
      .deleteTopics(topicNames.asJava)
      .all()
      .get(DefaultWaitSecs * topicNames.size, SECONDS)
  }

  def putRecordsJSON[T](topic: String, records: Seq[T], partitionKeyFunc: T => String)(
      implicit encoder: io.circe.Encoder[T]): Seq[RecordMetadata] =
    putRecords(topic, records.map(r => (partitionKeyFunc(r), encode(r))))

  def putRecords(topic: String, records: Seq[(String, String)]): Seq[RecordMetadata] =
    kafka.withStringProducer { producer =>
      records.map {
        case (key, payload) =>
          producer.send(new ProducerRecord(topic, key, payload)).get(DefaultWaitSecs, SECONDS)
      }
    }

  def pollRecordsJSON[T](consumerGroupID: String, topic: String, timeoutSecs: Int = 5)(
      implicit encoder: io.circe.Decoder[T]): Seq[(String, T)] =
    pollRecords(consumerGroupID, topic, timeoutSecs).map { case (key, event) => key -> decodeOrThrow[T](event) }

  def pollRecords(consumerGroupID: String, topic: String, timeoutSecs: Int = 5): Seq[(String, String)] =
    kafka.withStringConsumer(consumerGroupID) { consumer =>
      consumer.subscribe(singletonList(topic))
      val recs = consumer.poll(JDuration.ofSeconds(timeoutSecs)).asScala.toList
      consumer.commitSync()
      recs.map(r => r.key() -> r.value())
    }

  def autoClose[A <: AutoCloseable, B](autoCloseableFunc: A)(f: A ⇒ B): B = {
    try {
      f(autoCloseableFunc)
    } finally {
      autoCloseableFunc.close()
    }
  }

}
