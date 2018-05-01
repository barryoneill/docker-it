package com.hbc.dockerit.matchers

import com.hbc.dockerit.containers.RedisContainer
import com.hbc.dockerit.util.RedisUtil
import com.twitter.finagle.Redis
import com.twitter.finagle.redis.Client
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

trait RedisMatchers extends Matchers {
  container: RedisContainer =>

  lazy val redis: Client = Redis.newRichClient(s"localhost:$redisPort")

  def haveKeysMatching(pattern: String) = Matcher { (c: Client) =>
    val actualKeys = RedisUtil(c).keys(pattern)

    MatchResult(actualKeys.nonEmpty,
      s"""Expected at least one key to match "$pattern" but none was found""",
      s"""Expected at least one key to match "$pattern" but none was found"""
    )
  }

  def haveKeys(expectedKeys: String*) = Matcher { (c: Client) =>
    val actualKeys = RedisUtil(c).keys("*")

    val notFound = expectedKeys.filter(k => !actualKeys.contains(k))

    MatchResult(notFound.isEmpty,
      s"""Couldn't find key(s) "$notFound" in: $actualKeys""",
      s"""Couldn't find key(s) "$notFound" in: $actualKeys""",
    )
  }

  def haveOnlyKeys(expectedKeys: String*) = Matcher { (c: Client) =>
    val actualKeys = RedisUtil(c).keys("*")

    MatchResult(
      actualKeys.size == expectedKeys.size && actualKeys.toSet == expectedKeys.toSet,
      s"""Expected keys $expectedKeys but got $actualKeys""",
      s"""Expected keys $expectedKeys but got $actualKeys"""
    )
  }


}