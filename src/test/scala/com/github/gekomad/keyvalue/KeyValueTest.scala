package com.github.gekomad.keyvalue

import scala.concurrent.duration._

class KeyValueTest extends munit.FunSuite {

  test("test ttl") {
    val cache = KeyValue.add[String, String](GCtriggerMill = None, mainTLL = None)
    cache.set("key1", "value1", ttl = Some(100.millis))
    assert(cache.get("key1").contains("value1"))
    Thread.sleep(110)
    assert(cache.get("key1").isEmpty)
    assert(cache.size == 0)

    cache.set("key1", "value1", ttl = None)
    cache.invalidate()
    assert(cache.size == 0)

    cache.set("key2", "value2", ttl = Some(10.millis))

    Thread.sleep(11)
    cache.invalidate()
    assert(cache.size == 0)
  }

}
