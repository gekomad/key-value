
Key-value with TTL
--------------

Memoization function with TTL 
--------

Using Library: just add `KeyValue.scala` or `Memo.scala` to your project.

If primitive types are used, it is possible to avoid unboxing on key/value by adding the `@specialized` annotation.
### Memoize a function (some Scalaz code)
```scala
def myFunc(n: Int): Double = n + 1.0

import Memo._
val memoize = immutableHashMapMemo[Int, Double](ttl = Some(1.second), GCtriggerMill = Some(1.hour)) { n: Int =>
  Thread.sleep(500)
  myFunc(n)
}

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  println(s"time without memoization ${(System.currentTimeMillis() - time)} mill") // time without memoization 503 mill
}

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  println(s"time with memoization ${(System.currentTimeMillis() - time)} mill") // time with memoization 0 mill
}

Thread.sleep(1000)

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  println(s"time after ttl ${(System.currentTimeMillis() - time)} mill") // time after ttl 501 mill
}
```
### Key-value with TTL
```scala
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
```
### Memoize a function with external cache

```scala
def myFunc(n: Int, m: Double): String = {
  Thread.sleep(50)
  (n + m).toString
}
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

```
