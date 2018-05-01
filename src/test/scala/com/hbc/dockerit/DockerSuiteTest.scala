package com.hbc.dockerit

import com.hbc.dockerit.containers.RedisContainer
import com.hbc.dockerit.matchers.RedisMatchers
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
    redisUtil.mSet(Map("cat" -> "miaow", "dog" -> "woof", "duck" -> "quack", "cow" -> "moo", "human" -> "talk"))
  }

  "RedisContainer" should {

    "have matchers" that compare {

      "haveOnlyKeys" in {
        redis should haveOnlyKeys("cat", "dog", "duck", "cow", "human")
      }

      "haveKeys" in {
        redis should haveKeys("human", "dog")
      }

      "haveKeysMatching" in {
        redis should haveKeysMatching("*o*")
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
