package com.hbc.dockerit

import java.util.UUID

import com.hbc.dockerit.containers.KafkaContainer
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.KafkaUtil
import org.scalatest.WordSpec

class KafkaContainerSpec extends WordSpec with DockerSuite with KafkaContainer {

  lazy val kafkaUtil = KafkaUtil(Kafka)

  def compare: AfterWord = afterWord("compare")

  "KafkaContainer" should {

    "have utils" that {

      "can create, list and delete topics" in {

        val topics = Range(1, 4).map(a => s"util-test-$a-${UUID.randomUUID()}")

        val topicsBefore = kafkaUtil.listTopics()
        kafkaUtil.createTopics(topics: _*)

        kafkaUtil.listTopics() should contain theSameElementsAs (topicsBefore ++ topics)

      }

      "can send and read serialized, schemaless JSON objects" in {

        val testId = s"jsontest-${UUID.randomUUID()}"

        val topicName = s"schemaless-topic-$testId"

        kafkaUtil.createTopics(topicName)

        val rec1 = BankAccount("1234-RICH-GUY", 99999.99)
        val rec2 = BankAccount("1234-POOR-GUY", 00000.01)

        kafkaUtil.putRecordsJSON[BankAccount](
          topicName,
          Seq(rec1, rec2),
          acc => s"${acc.accountNumber}-${acc.amount}"
        )

        val fetched = kafkaUtil.pollRecordsJSON[BankAccount](s"group-$testId", topicName)

        fetched should equal(List(("1234-RICH-GUY-99999.99", rec1), ("1234-POOR-GUY-0.01", rec2)))

      }

      "can send and read strings" in {

        val testId = s"jsontest-${UUID.randomUUID()}"

        val topicName = s"schemaless-topic-$testId"
        println(topicName)

        kafkaUtil.createTopics(topicName)

        val records = Seq(
          "rec1Key" -> "this is the first record..",
          "rec2Key" -> ".. and the second.  We don't care that the contents aren't structured."
        )

        kafkaUtil.putRecords(topicName, records)

        val fetched = kafkaUtil.pollRecords(s"group-$testId", topicName)

        fetched should equal(records)

      }

    }

    // TODO: add matchers!

  }

}
