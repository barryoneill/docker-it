package com.hbc.dockerit.matchers

import com.hbc.dockerit.containers.DynamoDBContainer
import org.scalatest.Matchers

trait DynamoDBMatchers extends Matchers {
  container: DynamoDBContainer =>


}
