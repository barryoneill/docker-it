package com.hbc.dockerit.matchers

import com.hbc.dockerit.containers.RedisContainer
import com.hbc.dockerit.util.{ CirceSupport, RedisUtil }
import com.twitter.finagle.Redis
import com.twitter.finagle.redis.Client
import org.scalatest.Matchers
import org.scalatest.matchers.{ MatchResult, Matcher }

trait RedisMatchers extends Matchers with CirceSupport {
  container: RedisContainer =>

  lazy val redis: Client = Redis.newRichClient(s"localhost:$redisPort")

  def haveKeysMatching(pattern: String) = Matcher { c: Client =>
    val actualKeys = RedisUtil(c).keys(pattern).toList

    MatchResult(
      actualKeys.nonEmpty,
      s"""Expected at least one key to match "$pattern" but none did""",
      s"""Found ${actualKeys.size} keys matching "$pattern" where none were expected"""
    )
  }

  def haveKeys(expectedKeys: String*) = Matcher { c: Client =>
    val actualKeys = RedisUtil(c).keys("*")

    val notFound = expectedKeys.filter(k => !actualKeys.contains(k)).toList

    MatchResult(
      notFound.isEmpty,
      s"""Couldn't find key(s) "$notFound" in: $actualKeys""",
      s"""Unexpected key(s) "$expectedKeys" found in: $actualKeys""",
    )
  }

  def haveOnlyKeys(expectedKeys: String*) = Matcher { c: Client =>
    val actualKeys = RedisUtil(c).keys("*").toList

    MatchResult(
      actualKeys.size == expectedKeys.size && actualKeys.toSet == expectedKeys.toSet,
      s"""Expected keys $expectedKeys but got $actualKeys""",
      s"""Found only $actualKeys"""
    )
  }

  def haveValueOnGet(key: String, expected: String) = Matcher { c: Client =>
    RedisUtil(c).get(key) match {

      case Some(actual) =>
        MatchResult(
          actual == expected,
          s"""Expected value "$expected" but got "$actual" for key $key""",
          s"""Values match: "$expected" for key $key"""
        )

      case None =>
        MatchResult(matches = false, s"""No value found for $key""", s"""No value found for $key""")

    }
  }

  def haveEncodedValueOnGet[T](
    key: String,
    expectedObj: T
  )(implicit decoder: io.circe.Decoder[T], encoder: io.circe.Encoder[T]) = Matcher { c: Client =>
    val expectedJSON = encode[T](expectedObj)

    RedisUtil(c).get(key) match {

      case Some(actualJSON) =>
        val actualObj = decodeOrThrow[T](actualJSON)

        MatchResult(
          actualObj == expectedObj,
          s"""Expected value "$expectedJSON" but got "$actualJSON" for key $key""",
          s"""Values match: $expectedObj for key $key"""
        )

      case None =>
        MatchResult(matches = false, s"""No value found for $key""", s"""No value found for $key""")

    }
  }
}
