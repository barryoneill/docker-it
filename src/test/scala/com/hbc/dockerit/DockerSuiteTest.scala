package com.hbc.dockerit

import com.hbc.dockerit.containers.{LocalstackContainer, RedisContainer}
import com.hbc.dockerit.matchers.{CloudwatchMatchers, KinesisMatchers, RedisMatchers}
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.{KinesisUtil, RedisUtil}
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class DockerSuiteTest extends WordSpec with DockerSuite with BeforeAndAfterAll
  with RedisContainer with RedisMatchers
  with LocalstackContainer with KinesisMatchers with CloudwatchMatchers {

  def compare: AfterWord = afterWord("compare")

  object RedisTestData {
    val AnimalNoiseMap = Map("cat" -> "miaow", "dog" -> "woof", "duck" -> "quack", "human" -> "talk")
    val AccountKey = "savingsacc"
    val AccountVal = BankAccount("1234-RICH-GUY", 99999.99)

  }

  object KinesisTestData {
    val StreamName = "testStream"
    val BankAccounts = Seq(BankAccount("One", 1111), BankAccount("Two", 2222), BankAccount("Three", 333))
  }

  override def beforeAll(): Unit = {
    super.beforeAll()

    val redisUtil = RedisUtil(redis)
    redisUtil.resetCache()
    redisUtil.mSet(RedisTestData.AnimalNoiseMap)
    redisUtil.set(RedisTestData.AccountKey, RedisTestData.AccountVal)


  }

  "RedisContainer" should {

    "have redis matchers" that compare {

      "haveOnlyKeys" in {
        val expectedKeys = RedisTestData.AccountKey :: RedisTestData.AnimalNoiseMap.keySet.toList
        redis should haveOnlyKeys(expectedKeys : _*)
      }

      "haveKeys" in {
        redis should haveKeys("human", "dog")
        redis should not(haveKeys("platypus"))
      }

      "haveKeysMatching" in {
        redis should haveKeysMatching("*o*")
        redis should not(haveKeysMatching("fart"))
      }

      "haveValue (string)" in {
        redis should haveValueOnGet("dog", "woof")
      }

      "haveValue (encoded)" in {
        redis should haveEncodedValueOnGet(RedisTestData.AccountKey, RedisTestData.AccountVal)
      }
    }

  }

  "LocalstackContainer" should {

    "have kinesis matchers" that compare {

      "havePendingEvents" in {

        val kinesisUtil = KinesisUtil(kinesis)
        kinesisUtil.createStream(KinesisTestData.StreamName)
        kinesisUtil.putRecords[BankAccount](KinesisTestData.StreamName, KinesisTestData.BankAccounts, r => r.accountNumber)

        kinesis should havePendingEvents(KinesisTestData.StreamName, KinesisTestData.BankAccounts)

      }

    }

    "have cloudwatch matchers" that compare {

      "NOTHING (TODO)" in {

        // TODO: coming soon, eventually, at some point
        3 shouldBe 3
      }

    }

  }

}
