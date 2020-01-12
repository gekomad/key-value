package com.github.gekomad.keyvalue

import org.scalatest.funsuite.AnyFunSuite

import scala.concurrent.duration._
class KeyValueTest extends AnyFunSuite {

  test("test1") {
    val kv = KeyValue[String, String]()
    assert(kv.size("key1-a") == 0)
    assert(kv.size("key1-b") == 0)
    kv.set("key1-a", "key2-a", "value-a")
    kv.set("key1-b", "key2-b", "value-b")
    assert(kv.size("key1-a") == 1)
    assert(kv.size("key1-b") == 1)
    kv.delete("key1-a", "xx")
    kv.delete("key1-b", "xx")
    assert(kv.size("key1-a") == 1)
    assert(kv.size("key1-b") == 1)
  }

  test("test1.1") {
    val kv = KeyValue[String, String]()

    kv.set("key1-a", "key2-a", "value-a")
    kv.set("key1-b", "key2-b", "value-b")

    kv.delete("key1-a", "key2-a")
    assert(kv.size == 1)
    kv.delete("key1-b", "key2-b")
    assert(kv.size("key1-a") == 0)

    assert(kv.size("key1-b") == 0)
    assert(kv.size == 0)
  }

  test("test1.2") {
    val kv = KeyValue[String, String]()

    kv.set("key1-a", "key2-a", "value-a")
    kv.set("key1-a", "key2-a2", "value-a3")
    kv.set("key1-b", "key2-b", "value-b")

    kv.delete("key1-a", "key2-a")
    assert(kv.size == 2)
    kv.delete("key1-b", "key2-b")
    assert(kv.size("key1-a") == 1)

    assert(kv.size("key1-b") == 0)
    assert(kv.size == 1)
  }

  test("test2") {

    val kv = KeyValue[String, String]()

    kv.set("key1-a", "key2-a", "value-a")
    kv.set("key1-b", "key2-b", "value-b")

    kv.clear()
    assert(kv.size == 0)
  }

  test("test3") {

    val kv = KeyValue[String, String]()

    kv.set("key1-a", "key2-a", "value-a")
    kv.set("key1-a", "key2-a", "value-a")

    assert(kv.size("key1-a") == 1)
    assert(kv.get("key1-a", "key2-a") == Some("value-a"))
    kv.set("key1-a", "key2-a", "a4")
    assert(kv.size("key1-a") == 1)
    assert(kv.get("key1-a", "key2-a") == Some("a4"))

  }

  test("test4") {
    val kv = KeyValue[String, String]()
    kv.set("key1-a", "key2-a", "value-a")
    kv.set("key1-b", "key2-b", "value-b")

    assert(kv.size("key1-a") == 1)
    kv.clear("key1-a")
    assert(kv.size("key1-a") == 0)
    assert(kv.size("key1-b") == 1)
    assert(kv.size == 1)

  }

  test("test5") {
    val kv = KeyValue[Int, Double]()
    kv.clear("xx")
    assert(kv.size == 0)
  }

  test("test ttl") {
    val kv = KeyValue[String, String]()
    kv.set("key1-a", "key2-a", "value-a", ttl = Some(100.millis.toMillis))
    assert(kv.get("key1-a", "key2-a") == Some("value-a"))
    Thread.sleep(110)
    assert(kv.get("key1-a", "key2-a") == None)
    assert(kv.size == 0)
  }

  test("test auto ttl") {
    val kv = KeyValue[String, String](GCtriggerMill = 10.millis.toMillis)

    kv.set("key1-a", "key2-a", "value-a", ttl = Some(10))

    Thread.sleep(100)

    assert(kv.size == 0)
  }

}
