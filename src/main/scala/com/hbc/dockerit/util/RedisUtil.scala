package com.hbc.dockerit.util

import com.twitter.finagle.redis.Client
import com.twitter.finagle.redis.util.BufToString
import com.twitter.io.Buf

/**
  * A wrapper for the redis client, providing a bunch of synchronous convenience
  * methods to save clutter in tests/matchers.
  */
case class RedisUtil(client: Client) extends CirceSupport {

  import com.hbc.dockerit.TwitterFutureOps._

  def resetCache(timeoutSecs: Int = 5): Unit =
    client.flushAll().result(timeoutSecs)

  /**
    * Call 'keys', returning those that match the provided pattern
    */
  def keys(pattern: String, timeoutSecs: Int = 5): Seq[String] =
    client.keys(Buf.Utf8(pattern)).map(_.map(BufToString(_))).result(timeoutSecs)

  /**
    * Call 'mset' with the provided data as UTF8.  Returns self reference for chaining
    */
  def mSet(data: Map[String, String], timeoutSecs: Int = 5): Unit =
    client.mSet(mapToUTF8Buffer(data)).result(timeoutSecs)

  def hSet(key: String, data: Map[String, String], timeoutSecs: Int = 5): Unit =
    client.hMSet(Buf.Utf8(key), mapToUTF8Buffer(data)).result(timeoutSecs)

  private def mapToUTF8Buffer(data: Map[String, String]) = data.map {
    case (k, v) => Buf.Utf8(k) -> Buf.Utf8(v)
  }

  def get(key: String, timeoutSecs: Int = 5): Option[String] =
    client.get(Buf.Utf8(key)).result(timeoutSecs).map(BufToString(_))

  def getDecoded[T](key: String, timeoutSecs: Int = 5)(
    implicit decoder: io.circe.Decoder[T]
  ): Option[T] =
    get(key, timeoutSecs).map(v => decodeOrThrow[T](v))

  def set[T](key: String, value: T, timeoutSecs: Int = 5)(
    implicit encoder: io.circe.Encoder[T]
  ): Unit =
    client.set(Buf.Utf8(key), Buf.Utf8(encode(value))).result(timeoutSecs)
}
