package com.github.gekomad.keyvalue

import com.github.benmanes.caffeine.cache.Caffeine
import java.util.concurrent.{Executors, TimeUnit}
import scala.concurrent.duration.*
import scala.util.chaining.*

sealed abstract class MemoCaffeine[K, V] {
  def apply(z: K => V): K => V
  def invalidateAll(): Unit
}

object MemoCaffeine {
  private def memo[K, V](f: (K => V) => K => V, _invalidateAll: () => Unit): MemoCaffeine[K, V] =
    new MemoCaffeine[K, V] {
      override def apply(z: K => V): K => V = f(z)
      override def invalidateAll(): Unit    = _invalidateAll()
    }

  def memoCaffeine[K, V](
    ttl: Option[FiniteDuration] = None,
    maximumSize: Option[Long] = None,
    GCtriggerMill: Option[FiniteDuration] = None
  ): MemoCaffeine[K, V] = {
    val builder = Caffeine
      .newBuilder()
      .tap(b => maximumSize.foreach(b.maximumSize))
      .tap(b => ttl.foreach(d => b.expireAfterWrite(d.length, d.unit)))

    val cache = builder.build[K, V]()

    val a = memo[K, V](
      f => k => cache.get(k, (key: K) => f(key)),
      () => cache.invalidateAll()
    )
    GCtriggerMill.foreach { interval =>
      val scheduler = Executors.newSingleThreadScheduledExecutor { r =>
        val t = new Thread(r, "caffeine-cleanup-thread")
        t.setDaemon(true)
        t
      }
      scheduler.scheduleAtFixedRate(
        () => cache.cleanUp(),
        interval.toMillis,
        interval.toMillis,
        TimeUnit.MILLISECONDS
      )
    }

    a
  }
}
