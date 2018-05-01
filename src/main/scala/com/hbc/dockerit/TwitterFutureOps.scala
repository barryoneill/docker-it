package com.hbc.dockerit

object TwitterFutureOps {

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