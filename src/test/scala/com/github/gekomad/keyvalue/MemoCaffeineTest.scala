package com.github.gekomad.keyvalue

 
import com.github.gekomad.keyvalue.MemoCaffeine.memoCaffeine

import scala.concurrent.duration.*

class memoCaffeineTest extends munit.FunSuite {

  def myFunc(n: Int): Double = n + 1.0

  test("memoCaffeine with invalidateAll") {
    val memoFunc =
      memoCaffeine[Int, Double](ttl = Some(100.millis), maximumSize = None, GCtriggerMill = Some(1.hour))
    val memoize = memoFunc { (n: Int) =>
      Thread.sleep(50)
      myFunc(n)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time A $delay mill") // time without memoization is > 50 mills
      assert(delay >= 50)
    }

    memoFunc.invalidateAll()
    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time B $delay mill") // after invalidateAll time with memoization is > 50 mills
      assert(delay >= 50)
    }
  }

  test("memoCaffeine with ttl") {

    val memoize =
      memoCaffeine[Int, Double](ttl = Some(100.millis), maximumSize = None, GCtriggerMill = Some(1.hour)) {
        (n: Int) =>
          Thread.sleep(50)
          myFunc(n)
      }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time A $delay mill") // time without memoization > 50 mills
      assert(delay >= 50)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time B $delay mill") // time with memoization ~0 mills
      assert(delay < 50)
    }

    Thread.sleep(100)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time C $delay mill") // time after ttl > 50 mills
      assert(delay >= 50)
    }
  }

  test("memoCaffeine without ttl") {

    val memoize = memoCaffeine[Int, Double](ttl = None, maximumSize = None, GCtriggerMill = Some(1.hour)) {
      (n: Int) =>
        Thread.sleep(50)
        myFunc(n)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time D $delay mill") // time without memoization > 50 mills
      assert(delay >= 50)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time E $delay mill") // time with memoization ~0 mills
      assert(delay < 50)
    }

    Thread.sleep(100)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2.0)
      val delay = System.currentTimeMillis() - time
      println(s"time F $delay mill") // time with memoization 0 mills
      assert(delay < 50)
    }
  }

}
