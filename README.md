
Key-value with TTL
=====================

Memo with TTL (some Scalaz code)
=====================

## Using Library

Just add `KeyValue.scala` or `Memo.scala` to your project

### Memo with TTL
```
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
```
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
```
