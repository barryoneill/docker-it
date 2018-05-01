package com.hbc.dockerit

import com.hbc.dockerit.containers.{LocalstackContainer, RedisContainer}
import com.hbc.dockerit.matchers.{CloudwatchMatchers, KinesisMatchers, RedisMatchers}
import com.hbc.dockerit.util.RedisUtil
import org.scalatest.{Matchers, WordSpec}

class DockerSuiteTest extends WordSpec with Matchers with DockerSuite
                        with RedisContainer with RedisMatchers
                        with LocalstackContainer with KinesisMatchers with CloudwatchMatchers {

  "RedisContainer" should {

    "have working redis assertions" in {

      RedisUtil(redis).mSet(Map("cat" -> "miaow", "dog" -> "woof"))

      redis should haveKeys(Seq("cat","dog"))

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
