package com.github.gekomad.keyvalue

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._
class KeyValueTest extends AnyFunSuite {

  test("test ttl") {
    val kv = KeyValue[String, String](GCtriggerMill = Some(1.hour))
    kv.set("key1-a", "value1-a", ttl = Some(100.millis))
    assert(kv.get("key1-a") == Some("value1-a"))
    Thread.sleep(110)
    assert(kv.get("key1-a").isEmpty)
    assert(kv.size == 0)

    kv.set("key1-a", "value1-a", ttl = None)
    kv.clear()
    assert(kv.size == 0)

    kv.set("key2-a", "value2-a", ttl = Some(10.millis))

    Thread.sleep(11)

    assert(kv.size == 0)
  }

}
