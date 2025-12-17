package com.github.gekomad.keyvalue

import cats.effect.*
import cats.effect.kernel.Temporal
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*

private case class CacheEntry[V](value: V, expiresAt: FiniteDuration)

class CatsCache[K, V] private (cacheName: String, cache: Ref[IO, Map[K, CacheEntry[V]]], ttl: FiniteDuration) {
  private val logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO](s"CatsCache:$cacheName")

  def name: String = cacheName

  def get(key: K): IO[Option[V]] =
    Temporal[IO].monotonic.flatMap { now =>
      cache.modify { map =>
        map.get(key) match {
          case Some(CacheEntry(v, exp)) if now < exp => (map, Some(v))
          case Some(_)                               => (map - key, None)
          case None                                  => (map, None)
        }
      }
    }

  def upSert(key: K, value: V): IO[Unit] = upSert(key, value, ttl)

  def upSert(key: K, value: V, ttl1: FiniteDuration): IO[Unit] =
    for {
      _   <- logger.debug(s"$name upSert $key")
      now <- Temporal[IO].monotonic
      expiresAt = now + ttl1
      _ <- cache.update(_.updated(key, CacheEntry(value, expiresAt)))
    } yield ()

  def clean(): IO[Unit] =
    for {
      _   <- logger.debug(s"$name clean")
      now <- Temporal[IO].monotonic
      _   <- cache.update(_.filter((_, entry) => now < entry.expiresAt))
    } yield ()

  def append[A](key: K, values: Iterable[A])(using ev: V <:< Iterable[A]): IO[Unit] =
    for {
      now <- Temporal[IO].monotonic
      _ <- cache.update { map =>
        val current = map.get(key) match {
          case Some(CacheEntry(v, expiresAt)) if now < expiresAt => ev(v)
          case _                                                 => Nil
        }
        val updated = current ++ values
        map.updated(key, CacheEntry(updated.asInstanceOf[V], now + ttl))
      }
    } yield ()

  def delete(k: K): IO[Unit] =
    for {
      _ <- logger.debug(s"$name delete $name $k")
      _ <- cache.update(_ - k)
    } yield ()

  def empty(): IO[Unit] =
    for {
      _ <- logger.debug(s"$name empty")
      _ <- cache.set(Map.empty[K, CacheEntry[V]])
    } yield ()

  def size: IO[Int] = cache.get.map(_.size)

  def allKeys: IO[List[K]] = cache.get.map(_.keys.toList)
}

object CatsCache {

  def create[K, V](name: String, ttl: FiniteDuration, GCinterval: FiniteDuration): Resource[IO, CatsCache[K, V]] = {

    val logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO](s"CatsCache:$name")

    val cacheBuilder: IO[CatsCache[K, V]] = for {
      ref <- Ref.of[IO, Map[K, CacheEntry[V]]](Map.empty)
      cache = new CatsCache[K, V](cacheName = name, cache = ref, ttl = ttl)
    } yield cache

    def garbageCollector(cache: CatsCache[K, V]): IO[Unit] =
      (for {
        _     <- Temporal[IO].sleep(GCinterval)
        _     <- logger.info(s"CatsCache ${cache.name} Garbage Collector start")
        size1 <- cache.size
        _     <- if (size1 > 50) cache.clean() else IO(())
        size2 <- cache.size
        _     <- logger.info(s"CatsCache ${cache.name} Garbage Collector finish. Initial size: $size1, final: $size2")
      } yield ()).foreverM

    Resource
      .make(cacheBuilder) { cache =>
        logger.info(s"CatsCache ${cache.name} terminated.")
      }
      .flatTap { cache =>
        Resource.make(garbageCollector(cache).start)(_.cancel)
      }
  }
}
