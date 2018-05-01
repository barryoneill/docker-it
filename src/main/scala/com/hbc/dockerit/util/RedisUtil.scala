package com.hbc.dockerit.util

import com.twitter.finagle.redis.Client
import com.twitter.finagle.redis.util.BufToString
import com.twitter.io.Buf

/**
  * A wrapper for the redis client, providing a bunch of synchronous convenience
  * methods to save clutter in tests/matchers.
  */
case class RedisUtil(client: Client) {

  import com.hbc.dockerit.TwitterFutureOps._

  def resetCache(timeoutSecs: Int = 5): Unit = {
    client.flushAll().result(timeoutSecs)
  }

  /**
    * Call 'mset' with the provided data as UTF8.  Returns self reference for chaining
    */
  def mSet(data: Map[String, String], timeoutSecs: Int = 5): Unit = {

    val redisVals = data map { case (k, v) => Buf.Utf8(k) -> Buf.Utf8(v) }
    client.mSet(redisVals).result(timeoutSecs)
  }

  /**
    * Call 'keys', returning those that match the provided pattern
    */
  def keys(pattern: String, timeoutSecs: Int = 5): Seq[String] = {
    client.keys(Buf.Utf8(pattern)).map(_.map(BufToString(_))).result(timeoutSecs)
  }

}
