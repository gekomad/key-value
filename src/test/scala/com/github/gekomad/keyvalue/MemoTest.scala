package com.github.gekomad.keyvalue

import org.scalatest.FunSuite
import scala.concurrent.duration._

class MemoTest extends FunSuite {

  test("memo without ttl") {
    import Memo._
    val a = immutableHashMapMemo[Int, Int](ttl = None) { (n: Int) =>
      Thread.sleep(1000)
      n + 1
    }

    {
      val time = System.currentTimeMillis()
      assert(a.apply(1) == 2)
      println("timeA " + (System.currentTimeMillis() - time))
    }

    {
      val time = System.currentTimeMillis()
      assert(a(1) == 2)
      println("timeB " + (System.currentTimeMillis() - time))
    }
  }

  test("memo with ttl") {

    import Memo._
    val memoize = immutableHashMapMemo[Int, Int](ttl = Some(1.second.toMillis)) { (n: Int) =>
      Thread.sleep(2000)
      n + 1
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println("time A " + (System.currentTimeMillis() - time))
      assert(System.currentTimeMillis() - time > 1000)
    }

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println("time B " + (System.currentTimeMillis() - time))
      assert(System.currentTimeMillis() - time < 100)
    }

    Thread.sleep(2100)

    {
      val time = System.currentTimeMillis()
      assert(memoize(1) == 2)
      println("time C " + (System.currentTimeMillis() - time))
      assert(System.currentTimeMillis() - time > 1000)
    }
  }

}
