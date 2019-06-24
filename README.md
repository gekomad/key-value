
Key-value with TTL
=====================

Scalaz Memo with TTL
=====================

## Using Library

Just add `KeyValue.scala` or `Memo.scala` to your project

### Scalaz Memo with TTL
```
val memoize = immutableHashMapMemo[Int, Int](ttl = Some(1.second.toMillis)) { (n: Int) =>
    Thread.sleep(2000)
    n + 1
}

memoize(1) // result = 2 after 2 seconds
memoize(1) // result = 2 after 0 seconds
Thread.sleep(2100)
memoize(1) // result = 2 after 2 seconds
```
### Key-value with TTL
```
val kv = KeyValue[String, String]()
kv.set("key1-a", "key2-a", "value-a", ttl = Some(100.millis.toMillis))
kv.get("key1-a", "key2-a") // Some("value-a"))
Thread.sleep(110)
kv.get("key1-a", "key2-a") // None
```

### Test

`
sbt +test -J-XX:MaxMetaspaceSize=256M
`