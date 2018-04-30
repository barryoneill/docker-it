package com.hbc.dockerit

import com.twitter.finagle.Redis
import com.twitter.finagle.redis.Client
import com.twitter.finagle.redis.util.BufToString
import com.twitter.io.Buf

import scala.concurrent.Await
import scala.concurrent.duration._

trait RedisSpec {
  dockerSupport: DockerSupport =>

  import AsyncOps._

  def buildRedisClient: Client = Redis.newRichClient(s"localhost:$getMappedRedisPort")

  def redisGetAllKeys: Seq[String] = {
    val keyFut = buildRedisClient.keys(Buf.Utf8("*")).map(f => f.map(BufToString(_))).asScala
    Await.result(keyFut, 1.seconds)
  }
}

object AsyncOps {

  import com.twitter.util.{Future => TFuture}

  import scala.concurrent.{Promise, Future => SFuture}

  implicit class TwitterFutureOps[T](val f: TFuture[T]) extends AnyVal {
    def asScala: SFuture[T] = {
      val promise = Promise[T]()
      f.respond(t => promise.complete(t.asScala))
      promise.future
    }


  }

}