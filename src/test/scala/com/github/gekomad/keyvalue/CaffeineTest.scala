package com.github.gekomad.keyvalue

import cats.effect.Resource
import munit.CatsEffectSuite
import cats.effect.IO
import scala.concurrent.duration.DurationInt

class CaffeineTest extends CatsEffectSuite {
  test("CatsCaffeine test") {
    val testLogic = for {
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
    testLogic.use_
  }

  test("CatsCaffeine test GC") {
    val testLogic = for {
      cache <- CatsCaffeine[String, List[Int]](
        name = "CatsCaffeine",
        maximumSize = None,
        ttl = Some(400.millis),
        GCinterval = Some(800.millis)
      )
      _ <- Resource.eval {
        for {
          _  <- cache.upSert("a", List(1))
          s1 <- cache.size
          _ = assert(s1 == 1)
          a <- cache.get("a")
          _ = assert(a.contains(List(1)))
          _  <- IO.sleep(1.seconds)
          s2 <- cache.size
          _ = assert(s2 == 0)
        } yield ()
      }
    } yield ()
    testLogic.use_
  }

  test("CatsCaffeine test max size") {
    val testLogic = for {
      cache <- CatsCaffeine[Int, String](
        name = "CatsCaffeine",
        maximumSize = Some(1),
        ttl = None,
        GCinterval = None
      )
      _ <- Resource.eval {
        for {
          _ <- cache.upSert(1, "foo")
          s1 <- cache.size
          _ = assert(s1 == 1)
          _ <- cache.upSert(2, "bar")
          _  <- IO.sleep(1.seconds)
          s2 <- cache.size
          _ = assert(s2 == 1)
        } yield ()
      }
    } yield ()
    testLogic.use_
  }

}
