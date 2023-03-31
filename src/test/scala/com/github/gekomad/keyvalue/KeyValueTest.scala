package com.github.gekomad.keyvalue

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._
class KeyValueTest extends AnyFunSuite {

  test("test ttl") {
    val kv = KeyValue[String, String](GCtriggerMill = Some(1.hour))
    kv.set("key1", "value1", ttl = Some(100.millis))
    assert(kv.get("key1") == Some("value1"))
    Thread.sleep(110)
    assert(kv.get("key1").isEmpty)
    assert(kv.size == 0)

    kv.set("key1", "value1", ttl = None)
    kv.clear()
    assert(kv.size == 0)

    kv.set("key2", "value2", ttl = Some(10.millis))

    Thread.sleep(11)
    kv.gc()
    assert(kv.size == 0)
  }

}
