package com.hbc.dockerit.util

import io.circe.parser.decode
import io.circe.syntax._

trait CirceSupport {

  def decodeOrThrow[T](value: String)(implicit decoder: io.circe.Decoder[T]): T =
    decode[T](value) match {
      case Right(t) => t
      case Left(e)  => throw new RuntimeException(s"""Couldn't parse "$value"""", e)
    }

  def encode[T](value: T)(implicit decoder: io.circe.Encoder[T]): String = value.asJson.noSpaces

}
