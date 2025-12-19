
Key-value with TTL
--------------

Memoization function with TTL 
--------

Using Library: just add `KeyValue.scala`, `Memo.scala`, `CatsCache.scala`, `CatsCaffeine.scala` or `MemoCaffeine.scala` to your project.

If primitive types are used, it is possible to avoid unboxing on key/value by adding the `@specialized` annotation.
### Memoize a function (some Scalaz code)
```scala

import scala.concurrent.duration.DurationInt

def myFunc(n: Int): Double = n + 1.0

import Memo._

val memoize = immutableHashMapMemo[Int, Double](ttl = Some(1.second), GCtriggerMill = Some(1.hour)) {
  (n: Int) =>
    Thread.sleep(500)
    myFunc(n)
}

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  println(
    s"time without memoization ${(System.currentTimeMillis() - time)} mill"
  ) // time without memoization is > 500 mills
}

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  println(s"time with memoization ${(System.currentTimeMillis() - time)} mill") // time with memoization is ~0 mills
}

Thread.sleep(1000)

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  println(s"time after ttl ${(System.currentTimeMillis() - time)} mill") // time after expired is > 500 mills
}
```
### Key-value with TTL
```scala

import scala.concurrent.duration.DurationInt

val kv = KeyValue.add[String, String](GCtriggerMill = None, mainTLL = None)
kv.set("key1", "value1", ttl = Some(100.millis))
assert(kv.get("key1").contains("value1"))
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
    
import scala.concurrent.duration.DurationInt
import com.github.gekomad.keyvalue.KeyValue.keyValue

def myFunc(n: Int, m: Double): String = {
  Thread.sleep(50)
  (n + m).toString
}

val cache = new keyValue[(Int, Double), String](GCtriggerMill = Some(1.hour), mainTLL = Some(500.millis))
val myFuncMemoized = KeyValue.meomizeFunc(cache)(myFunc)

{
  val time = System.currentTimeMillis()
  assert(myFuncMemoized(1, 2.0) == "3.0")
  assert(cache.size == 1)
  val delay = System.currentTimeMillis() - time
  assert(delay >= 50) // first time the cache is not used
}

{
  val time = System.currentTimeMillis()
  assert(myFuncMemoized(1, 2.0) == "3.0")
  val delay = System.currentTimeMillis() - time
  assert(delay < 50) // second time the cache is used
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
### Key-value with Cats

```scala

import scala.concurrent.duration.DurationInt
import cats.effect.kernel.Resource

for {
  cache <- CatsCache.create[String, Int](name = "CatsCache", ttl = 1.minutes, GCinterval = 5.hour)
  _ <- Resource.eval {
    for {
      _ <- cache.upSert("a", 1)
      s1 <- cache.size
      _ = assert(s1 == 1)
      a <- cache.get("a")
      _ = assert(a.contains(1))
      _ <- cache.delete("a")
      s2 <- cache.size
      _ = assert(s2 == 0)
    } yield ()
  }
} yield ()
```

```scala
import scala.concurrent.duration.DurationInt
import cats.effect.kernel.Resource

for {
  cache <- CatsCache.create[String, List[Int]](name = "CatsCache", ttl = 1.minutes, GCinterval = 5.hour)
  _ <- Resource.eval {
    for {
      _  <- cache.upSert("a", List(1))
      s1 <- cache.size
      _ = assert(s1 == 1)
      _ <- cache.append("a", List(2))
      a <- cache.get("a")
      _ = assert(a.contains(List(1, 2)))
    } yield ()
  }
} yield ()
```

Using [Caffeine](https://github.com/ben-manes/caffeine) high performance caching Java library.

### Key-value with Cats and Caffeine

```scala
 import scala.concurrent.duration.DurationInt
import cats.effect.IO
import cats.effect.kernel.Resource

def myFunc(n: Int): Double = n + 1.0

for {
  cache <- CatsCaffeine[Int, String](
    name = "CatsCaffeine",
    maximumSize = None,
    ttl = Some(400.millis),
    GCinterval = None
  )
  _ <- Resource.eval {
    for {
      _ <- cache.upSert(1, "foo")
      a <- cache.get(1)
      _ = assert(a.contains("foo"))
      _ <- IO.sleep(1.seconds)
      b <- cache.get(1)
      _ = assert(b.isEmpty)
    } yield ()
  }
} yield ()
```

### Memoize a function with with Cats and Caffeine

```scala

import com.github.gekomad.keyvalue.MemoCaffeine.memoCaffeine
import scala.concurrent.duration.DurationInt

def myFunc(n: Int): Double = n + 1.0

val memoize = memoCaffeine[Int, Double](ttl = Some(100.millis), maximumSize = None, GCtriggerMill = Some(1.hour)) {
  (n: Int) =>
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

{
  val time = System.currentTimeMillis()
  assert(memoize(1) == 2.0)
  val delay = System.currentTimeMillis() - time
  println(s"time B $delay mill") // time with memoization is ~0 mills
  assert(delay < 50)
}
```

### Memoize a function with with Cats and Caffeine using `invalidateAll`

```scala
import com.github.gekomad.keyvalue.MemoCaffeine.memoCaffeine
import scala.concurrent.duration.DurationInt

def myFunc(n: Int): Double = n + 1.0

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

```
Further examples [here](https://github.com/gekomad/key-value/tree/master/src/test/scala/com/github/gekomad/keyvalue)