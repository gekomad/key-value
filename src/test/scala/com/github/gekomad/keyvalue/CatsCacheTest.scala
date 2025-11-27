package com.github.gekomad.keyvalue

import com.github.gekomad.keyvalue.CatsCache.CatsCache
import cats.effect.unsafe.implicits.global
import scala.concurrent.duration.*

class CatsCacheTest extends munit.FunSuite {

  test("cats test") {
    val cache: CatsCache[String, Int] =
      CatsCache.create[String, Int]("cache1", ttl = 1.minutes, GCinterval = 5.hour).unsafeRunSync()

    val a = for {
      _ <- cache.upSert("a", 1)
      s1 <- cache.size
      a <- cache.get("a")
      _ <- cache.delete("a")
      s2 <- cache.size
    } yield {
      assert(s1 == 1)
      assert(s2 == 0)
      assert(a.contains(1))
    }

    a.unsafeRunSync()

  }
}
