package com.hbc.dockerit.util

import com.twitter.finagle.redis.Client
import com.twitter.io.Buf

import scala.concurrent.Await

case class RedisUtil(client: Client) {

  import com.hbc.dockerit.TwitterFutureOps._

  import scala.concurrent.duration._

  def mSet(data: Map[String, String]): Unit = {

    val redisVals = data map { case (k, v) => Buf.Utf8(k) -> Buf.Utf8(v) }
    Await.ready(client.mSet(redisVals).asScala, 5.seconds)
  }

}
