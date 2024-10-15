package com.github.gekomad.keyvalue

import scala.concurrent.duration._

class KeyValueTest extends munit.FunSuite {

  test("test ttl") {
    val kv = KeyValue.add[String, String](GCtriggerMill = None, mainTLL = None)
    kv.set("key1", "value1", ttl = Some(100.millis))
    assert(kv.get("key1") == Some("value1"))
    Thread.sleep(110)
    assert(kv.get("key1").isEmpty)
    assert(kv.size == 0)

    kv.set("key1", "value1", ttl = None)
    kv.invalidate()
    assert(kv.size == 0)

    kv.set("key2", "value2", ttl = Some(10.millis))

    Thread.sleep(11)
    kv.invalidate()
    assert(kv.size == 0)
  }

}
