package com.github.gekomad.keyvalue

import cats.effect.*
import cats.effect.kernel.Temporal
import cats.syntax.all.*
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import scala.concurrent.duration.*
import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import com.github.benmanes.caffeine.cache.stats.CacheStats

class CatsCaffeine[K, V] private (cacheName: String, cache: Ref[IO, Cache[K, V]]) {
  private val logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO](s"CatsCaffeine:$cacheName")

  extension [K, V](c: Cache[K, V]) {
    def get(k: K): IO[Option[V]] = IO(Option(c.getIfPresent(k)))
  }

  def name: String = cacheName

  def get(key: K): IO[Option[V]] =
    for {
      c <- cache.get
      v <- c.get(key)
    } yield v

  def upSert(key: K, value: V): IO[Unit] =
    for {
      _ <- logger.debug(s"$name upSert $key")
      c <- cache.get
    } yield c.put(key, value)

  def clean(): IO[Unit] =
    for {
      _ <- logger.debug(s"$name clean")
      c <- cache.get
    } yield c.cleanUp()
 
  def delete(k: K): IO[Unit] =
    for {
      _ <- logger.debug(s"$name delete $name $k")
      c <- cache.get
    } yield c.invalidate(k)

  def empty(): IO[Unit] =
    for {
      _ <- logger.debug(s"$name empty")
      c <- cache.get
    } yield c.invalidateAll()

  def size: IO[Long] =
    for {
      _ <- logger.debug(s"$name size")
      c <- cache.get
    } yield c.estimatedSize()

  def stats: IO[CacheStats] =
    for {
      _ <- logger.debug(s"$name stats")
      c <- cache.get
    } yield c.stats()

}

object CatsCaffeine {

  import scala.util.chaining.*
  def apply[K, V](
    name: String,
    ttl: Option[FiniteDuration],
    maximumSize: Option[Long],
    GCinterval: Option[FiniteDuration]
  ): Resource[IO, CatsCaffeine[K, V]] = {
    val logger: Logger[IO] = Slf4jLogger.getLoggerFromName[IO](s"CatsCaffeine:$name")
    val cacheBuilder: IO[CatsCaffeine[K, V]] = for {
      ref <- Ref.of[IO, Cache[K, V]] {
        Caffeine
          .newBuilder()
          .tap(b => maximumSize.foreach(b.maximumSize))
          .tap(b => ttl.foreach(v => b.expireAfterWrite(v.length, v.unit)))
          .build[K, V]
      }
      cache = new CatsCaffeine[K, V](cacheName = name, cache = ref)
    } yield cache

    def garbageCollector(gc: FiniteDuration, cache: CatsCaffeine[K, V]): IO[Unit] =
      (for {
        _     <- Temporal[IO].sleep(gc)
        _     <- logger.info(s"CatsCaffeine ${cache.name} Garbage Collector start")
        size1 <- cache.size
        _     <- cache.clean()
        size2 <- cache.size
        _ <- logger.info(
          s"CatsCaffeine ${cache.name} Garbage Collector finish. Initial estimated size: $size1, final: $size2"
        )
      } yield ()).foreverM

    val a = Resource
      .make(cacheBuilder) { cache =>
        logger.info(s"${cache.name} terminated.")
      }
    GCinterval match {
      case Some(value) =>
        a.flatTap { cache =>
          Resource.make(garbageCollector(value, cache).start)(_.cancel)
        }
      case None => a
    }

  }
}
