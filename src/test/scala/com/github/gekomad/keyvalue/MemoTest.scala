package com.github.gekomad.keyvalue

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._

class MemoTest extends AnyFunSuite {

  test("memo with ttl") {
    import Memo._
    val memoize = immutableHashMapMemo[Int, Int](ttl = Some(100.millis), GCtriggerMill = Some(1.hour)) { n: Int =>
      Thread.sleep(50)
      n + 1
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      val delay = System.currentTimeMillis() - time
      println(s"time A $delay mill") // time without memoization 52 mills
      assert(delay >= 50)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      val delay = System.currentTimeMillis() - time
      println(s"time B $delay mill") // time with memoization 0 mills
      assert(delay < 50)
    }

    Thread.sleep(100)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      val delay = System.currentTimeMillis() - time
      println(s"time C $delay mill") // time after ttl 52 mills
      assert(delay >= 50)
    }
  }

  test("memo without ttl") {

    import Memo._
    val memoize = immutableHashMapMemo[Int, Int](ttl = None, GCtriggerMill = Some(1.hour)) { n: Int =>
      Thread.sleep(50)
      n + 1
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      val delay = System.currentTimeMillis() - time
      println(s"time D $delay mill") // time without memoization 52 mills
      assert(delay >= 50)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      val delay = System.currentTimeMillis() - time
      println(s"time E $delay mill") // time with memoization 0 mills
      assert(delay < 50)
    }

    Thread.sleep(100)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      val delay = System.currentTimeMillis() - time
      println(s"time F $delay mill") // time with memoization 0 mills
      assert(delay < 50)
    }
  }

}
