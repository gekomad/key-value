
Key-value with TTL
=====================

Memo with TTL (some Scalaz code)
=====================

## Using Library

Just add `KeyValue.scala` or `Memo.scala` to your project

### Memo with TTL
```scala
import Memo._
val memoize = immutableHashMapMemo[Int, Int](ttl = Some(1.second), GCtriggerMill = Some(1.hour)) { n: Int =>
  Thread.sleep(500)
  n + 1
}

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2)
  println(s"time without memoization ${(System.currentTimeMillis() - time)} mill") // time without memoization 503 mill
}

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2)
  println(s"time with memoization ${(System.currentTimeMillis() - time)} mill") // time with memoization 0 mill
}

Thread.sleep(1000)

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2)
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
