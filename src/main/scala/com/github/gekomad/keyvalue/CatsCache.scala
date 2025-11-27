package com.github.gekomad.keyvalue

import cats.effect.IO
import cats.effect.{Ref, Temporal}
import cats.syntax.all.*
import scala.concurrent.duration.*

object CatsCache {

  private case class CacheEntry[A](value: A, expiresAt: FiniteDuration)

  class CatsCache[K, V] private (name1: String, cache: Ref[IO, Map[K, CacheEntry[V]]], ttl: FiniteDuration) {
    def name: String = name1
    def get(key: K): IO[Option[V]] =
      for {
        now <- Temporal[IO].monotonic
        map <- cache.get
        entry = map.get(key)
        value <- entry match {
          case Some(CacheEntry(v, expiresAt)) if now < expiresAt => v.some.pure[IO]
          case _                                                 => delete(key) >> none[V].pure[IO]
        }
      } yield value

    def upSert(key: K, value: V): IO[Unit] = // idenpotent
      for {
        now <- Temporal[IO].monotonic
        expiresAt = now + ttl
        _ <- cache.update { a =>
          a.get(key) match {
            case Some(found) if now > expiresAt =>
              a.updated(key, CacheEntry(found.value, expiresAt)) // update value and ttl
            case None => a.updated(key, CacheEntry(value, expiresAt))
            case _    => a
          }
        }
      } yield ()

    def clean(): IO[Unit] =
      for {
        now <- Temporal[IO].monotonic
        _   <- cache.update(_.filter { case (_, entry) => now < entry.expiresAt })
      } yield ()

    def delete(k: K): IO[Unit] = cache.update(_ - k)

    def empty(): IO[Unit] = cache.set(Map.empty[K, CacheEntry[V]])

    def size: IO[Int]        = cache.get.map(_.size)
    def allKeys: IO[List[K]] = cache.get.map(_.keys.toList)
  }

  object CatsCache {
    private def garbageCollector[K, V](cache: CatsCache[K, V], GCinterval: FiniteDuration): IO[Unit] =
      (for {
        _     <- Temporal[IO].sleep(GCinterval)
        size1 <- cache.size
        _     <- cache.clean()
        size2 <- cache.size
        _     <- IO.println(s"CatsCache ${cache.name} garbage collector. Initial size: $size1 final size: $size2")
      } yield ()).foreverM

    def create[K, V](name: String, ttl: FiniteDuration, GCinterval: FiniteDuration): IO[CatsCache[K, V]] = {
      val cache = Ref.of[IO, Map[K, CacheEntry[V]]](Map.empty).map { ref =>
        new CatsCache[K, V](name, ref, ttl)
      }
      for {
        a <- cache
        _ <- garbageCollector(a, GCinterval).start
      } yield a
    }
  }
}
