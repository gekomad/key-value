package com.github.gekomad.keyvalue

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._

class MemoTest extends AnyFunSuite {

  test("memo without ttl") {
    import Memo._
    val memoize = immutableHashMapMemo[Int, Int](ttl = Some(100.millis), GCtriggerMill = Some(1.hour)) { n: Int =>
      Thread.sleep(50)
      n + 1
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println(s"time without memoization ${System.currentTimeMillis() - time} mill") // time without memoization 503 mill
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println(s"time with memoization ${System.currentTimeMillis() - time} mill") // time with memoization 0 mill
    }

    Thread.sleep(250)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println(s"time after ttl ${System.currentTimeMillis() - time} mill") // time after ttl 501 mill
    }
  }

  test("memo with ttl") {

    import Memo._
    val memoize = immutableHashMapMemo[Int, Int](ttl = Some(250.millis), GCtriggerMill = None) { n: Int =>
      Thread.sleep(500)
      n + 1
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println("time A " + (System.currentTimeMillis() - time))
      assert(System.currentTimeMillis() - time > 250)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println("time B " + (System.currentTimeMillis() - time))
      assert(System.currentTimeMillis() - time < 25)
    }

    Thread.sleep(750)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println("time C " + (System.currentTimeMillis() - time))
      assert(System.currentTimeMillis() - time > 250)
    }
  }

}
