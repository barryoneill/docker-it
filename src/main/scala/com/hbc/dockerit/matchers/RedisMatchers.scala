package com.hbc.dockerit.matchers

import com.hbc.dockerit.containers.RedisContainer
import com.twitter.finagle.Redis
import com.twitter.finagle.redis.Client
import com.twitter.finagle.redis.util.BufToString
import com.twitter.io.Buf
import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.concurrent.Await
import scala.concurrent.duration._

trait RedisMatchers extends Matchers {
  container: RedisContainer =>

  import com.hbc.dockerit.TwitterFutureOps._

  lazy val redis: Client = Redis.newRichClient(s"localhost:$redisPort")

  def haveKeys(expectedKeys: Seq[String]) = new RedisKeyMatcher(expectedKeys)

  class RedisKeyMatcher(expectedKeys: Seq[String]) extends Matcher[Client] {

    def apply(left: Client): MatchResult = {

      val keyFut = redis.keys(Buf.Utf8("*")).map(f => f.map(BufToString(_))).asScala
      val actualKeys = Await.result(keyFut, 5.seconds)

      MatchResult(
        actualKeys.size == expectedKeys.size && actualKeys.toSet == expectedKeys.toSet,
        s"""Expected keys $expectedKeys but got $actualKeys""",
        s"""Expected keys $expectedKeys but got $actualKeys"""
      )
    }
  }

}