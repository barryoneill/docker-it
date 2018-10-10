package com.hbc.dockerit.matchers

import com.hbc.dockerit.containers.KafkaContainer
import com.hbc.dockerit.util.CirceSupport
import org.scalatest.Matchers

trait KafkaMatchers extends Matchers with CirceSupport {
  container: KafkaContainer =>

  // coming soon

}
