package com.github.gekomad.keyvalue

import com.github.gekomad.keyvalue.KeyValue.keyValue

import scala.concurrent.duration.*

class MemoKeyValueTest extends munit.FunSuite {
  def myFunc(n: Int, m: Double): String = {
    Thread.sleep(50)
    (n + m).toString
  }

  test("test memo key value") {

    val cache          = new keyValue[(Int, Double), String](GCtriggerMill = Some(1.hour), mainTLL = Some(500.millis))
    val myFuncMemoized = KeyValue.meomizeFunc(cache)(myFunc)

    {
      val time = System.currentTimeMillis()
      assert(myFuncMemoized(1, 2.0) == "3.0")
      assert(cache.size == 1)
      val delay = System.currentTimeMillis() - time
      assert(delay >= 50)  // first time the cache is not used
    }

    {
      val time = System.currentTimeMillis()
      assert(myFuncMemoized(1, 2.0) == "3.0")
      val delay = System.currentTimeMillis() - time
      assert(delay < 50)  // second time the cache is used
    }

    {
      val time1 = System.currentTimeMillis()
      Thread.sleep(501)
      assert(myFuncMemoized(1, 2.0) == "3.0")
      val delay1 = System.currentTimeMillis() - time1
      assert(delay1 >= 50)      
      cache.invalidate()
      assert(cache.size == 0)
      val time2 = System.currentTimeMillis()
      assert(myFuncMemoized(1, 2.0) == "3.0")
      val delay2 = System.currentTimeMillis() - time2
      assert(delay2 >= 50)
    }
  }

}
