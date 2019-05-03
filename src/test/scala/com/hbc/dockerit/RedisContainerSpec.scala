package com.hbc.dockerit

import com.hbc.dockerit.containers.RedisContainer
import com.hbc.dockerit.models.BankAccount
import com.hbc.dockerit.util.RedisUtil
import org.scalatest.WordSpec

class RedisContainerSpec extends WordSpec with DockerSuite with RedisContainer {

  def compare: AfterWord = afterWord("compare")

  object RedisTestData {
    val AnimalNoiseMap =
      Map("cat" -> "miaow", "dog" -> "woof", "duck" -> "quack", "human" -> "talk")
    val AccountKey = "savingsacc"
    val AccountVal = BankAccount("1234-RICH-GUY", 99999.99)
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
        redis should haveOnlyKeys(expectedKeys: _*)
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

}
