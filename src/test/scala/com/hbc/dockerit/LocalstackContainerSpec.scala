package com.hbc.dockerit

import com.hbc.dockerit.containers.LocalStackContainer
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.KinesisUtil
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class LocalstackContainerSpec extends WordSpec with BeforeAndAfterAll
                    with DockerSuite with LocalStackContainer {

  def compare: AfterWord = afterWord("compare")

  object KinesisTestData {
    val StreamName = "testStream"
    val BankAccounts = Seq(BankAccount("One", 1111), BankAccount("Two", 2222), BankAccount("Three", 333))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    val kinesisUtil = KinesisUtil(kinesis)
    kinesisUtil.createStream(KinesisTestData.StreamName)
    kinesisUtil.putRecordsJSON[BankAccount](KinesisTestData.StreamName, KinesisTestData.BankAccounts, r => r.accountNumber)
  }

  "LocalstackContainer" should {

    "have kinesis matchers" that compare {

      "havePendingEvents" in {
        kinesis should havePendingEvents(KinesisTestData.StreamName, KinesisTestData.BankAccounts)
      }

    }

    //    "have cloudwatch matchers" that compare {
    //
    //      "havePublishedMetrics" in {
    //
    //        // TODO: coming soon, eventually, at some point
    //        3 shouldBe 3
    //      }
    //
    //    }

  }


}
