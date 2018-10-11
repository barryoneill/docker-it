package com.hbc.dockerit.util

object JavaUtils {

  def autoClose[A <: AutoCloseable, B](autoCloseableFunc: A)(f: A ⇒ B): B = {
    try {
      f(autoCloseableFunc)
    } finally {
      autoCloseableFunc.close()
    }
  }
}
