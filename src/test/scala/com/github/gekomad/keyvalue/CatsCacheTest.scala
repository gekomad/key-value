package com.github.gekomad.keyvalue

import cats.effect.Resource
import munit.CatsEffectSuite

import scala.concurrent.duration.*

class CatsCacheTest extends CatsEffectSuite {

  test("CatsCache test") {
    val testLogic = for {
      cache <- CatsCache.create[String, Int](name = "CatsCache", ttl = 1.minutes, GCinterval = 5.hour)
      _ <- Resource.eval {
        for {
          _  <- cache.upSert("a", 1)
          s1 <- cache.size
          _ = assert(s1 == 1)
          a <- cache.get("a")
          _ = assert(a.contains(1))
          _  <- cache.delete("a")
          s2 <- cache.size
          _ = assert(s2 == 0)
        } yield ()
      }
    } yield ()
    testLogic.use_
  }

  test("CatsCache test list") {
    val testLogic = for {
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
    testLogic.use_
  }

}
