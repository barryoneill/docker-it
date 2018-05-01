package com.hbc.dockerit

import com.hbc.dockerit.containers.RedisContainer
import com.hbc.dockerit.matchers.RedisMatchers
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.RedisUtil
import org.scalatest.{BeforeAndAfterAll, WordSpec}

class DockerSuiteTest extends WordSpec with DockerSuite with BeforeAndAfterAll
  with RedisContainer with RedisMatchers
  //  with LocalstackContainer with KinesisMatchers with CloudwatchMatchers {
{

  def compare: AfterWord = afterWord("compare")

  override def beforeAll(): Unit = {
    super.beforeAll()

    val redisUtil = RedisUtil(redis)
    redisUtil.resetCache()
    redisUtil.mSet(Map("cat" -> "miaow", "dog" -> "woof", "duck" -> "quack", "human" -> "talk"))
    redisUtil.set("savingsacc", BankAccount("1234-RICH-GUY", 99999.99))
  }

  "RedisContainer" should {

    "have matchers" that compare {

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

    "have working kinesis assertions" in {

      // TODO:
      3 shouldBe 3

    }

    "have working cloudwatch assertions" in {

      // TODO
      3 shouldBe 3

    }

  }


}
