package com.hbc.dockerit.matchers

import com.hbc.dockerit.containers.PostgresContainer
import org.scalatest.Matchers

trait PostgresMatchers extends Matchers {
  container: PostgresContainer =>


}
