package com.hbc.dockerit

import scala.concurrent.Await
import scala.concurrent.duration._

object TwitterFutureOps {

  import com.twitter.util.{ Future => TFuture }

  import scala.concurrent.{ Promise, Future => SFuture }

  implicit class TwitterFutureOps[T](val f: TFuture[T]) extends AnyVal {
    def asScala: SFuture[T] = {
      val promise = Promise[T]()
      f.respond(t => promise.complete(t.asScala))
      promise.future
    }

    def result(timeoutSecs: Int = 5): T =
      Await.result(f.asScala, timeoutSecs.seconds)
  }

}
