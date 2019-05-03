package com.hbc.dockerit

object errors {

  sealed class Error(msg: String, cause: Option[Throwable]) extends Throwable(msg, cause.orNull)

  case class DynamoDBError(
    msg: String = "dynamodb error occurred",
    cause: Option[Throwable] = None
  ) extends Error(msg, cause)

}
