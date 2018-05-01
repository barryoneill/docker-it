package com.hbc.dockerit

import com.hbc.dockerit.containers.{LocalstackContainer, RedisContainer}
import com.hbc.dockerit.matchers.{CloudwatchMatchers, KinesisMatchers, RedisMatchers}
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.RedisUtil
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class DockerSuiteTest extends WordSpec with DockerSuite with BeforeAndAfterAll
  with RedisContainer with RedisMatchers
  with LocalstackContainer with KinesisMatchers with CloudwatchMatchers {

  def compare: AfterWord = afterWord("compare")

  override def beforeAll(): Unit = {
    super.beforeAll()

    val redisUtil = RedisUtil(redis)
    redisUtil.resetCache()
    redisUtil.mSet(Map("cat" -> "miaow", "dog" -> "woof", "duck" -> "quack", "human" -> "talk"))
    redisUtil.set("savingsacc", BankAccount("1234-RICH-GUY", 99999.99))
  }

  "RedisContainer" should {

    "have redis matchers" that compare {

      "haveOnlyKeys" in {
        redis should haveOnlyKeys("cat", "dog", "duck", "human", "savingsacc")
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
        redis should haveEncodedValueOnGet("savingsacc", BankAccount("1234-RICH-GUY", 99999.99))
      }
    }

  }

  "LocalstackContainer" should {

    "have kinesis matchers" that compare {

      "havePendingEvents" in {

        // TODO:
        3 shouldBe 3

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
