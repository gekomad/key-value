/**
  * Scalaz Memo with TTL
  */
package com.github.gekomad.keyvalue

import java.util.concurrent.Executors

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}

class Value[@specialized(Int, Long, Double) V](val value: V, val ttl: Option[FiniteDuration]) {
  val created: Long = System.currentTimeMillis()
}

/** A function memoization strategy.  See companion for various
  * instances employing various strategies.
  */
sealed abstract class Memo[@specialized(Int) K, @specialized(Int, Long, Double) V] {
  def apply(z: K => V): K => V
}

/** @define immuMapNote As this memo uses a single var, it's
  * thread-safe. */
object Memo {
  private def memo[@specialized(Int) K, @specialized(Int, Long, Double) V](f: (K => V) => K => V): Memo[K, V] =
    new Memo[K, V] {
      override def apply(z: K => V): K => V = f(z)
    }

  def nilMemo[@specialized(Int) K, @specialized(Int, Long, Double) V]: Memo[K, V] =
    memo[K, V](f => k => new Value(f(k), None).value)

  import collection.mutable

  private def mutableMapMemo[K, V](a: mutable.Map[K, V]): Memo[K, V] =
    memo[K, V](f => k => new Value(a.getOrElseUpdate(k, f(k)), None).value)

  /** Cache results in a [[scala.collection.mutable.HashMap]].
    * Nonsensical if `K` lacks a meaningful `hashCode` and
    * `java.lang.Object.equals`.
    */
  def mutableHashMapMemo[K, V]: Memo[K, V] =
    mutableMapMemo(new mutable.HashMap[K, V])

  /** As with `mutableHashMapMemo`, but forget elements according to
    * GC pressure.
    */
  def weakHashMapMemo[K, V]: Memo[K, V] =
    mutableMapMemo(new mutable.WeakHashMap[K, V])

  import collection.immutable

  private def immutableMapMemo[K, V](
    m: immutable.Map[K, Value[V]],
    ttl: Option[FiniteDuration],
    GCtriggerMill: Option[FiniteDuration]
  ): Memo[K, V] = {
    var map = m

    implicit val ec: ExecutionContextExecutorService =
      ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(1))

    GCtriggerMill.foreach { d =>
      Future {
        while (true) {
          Thread.sleep(d.toMillis)
          map.foreach {
            case (key, value) =>
              value.ttl match {
                case Some(time) => if ((System.currentTimeMillis() - value.created) > time.toMillis) map -= key
                case None       => ()
              }
          }
        }
      }
    }

    memo[K, V](
      f =>
        k => {
          val optValue = map get k
          val value = optValue.getOrElse {
            val x = new Value(f(k), ttl)
            map = map.updated(k, x)
            x
          }
          value.ttl match {
            case Some(time) =>
              if ((System.currentTimeMillis() - value.created) > time.toMillis) {
                val x = new Value(f(k), ttl)
                map = map.updated(k, x)
                x.value
              } else value.value
            case None => value.value
          }
      }
    )
  }

  /** Cache results in a hash map.  Nonsensical unless `K` has
    * a meaningful `hashCode` and `java.lang.Object.equals`.
    * $immuMapNote
    */
  def immutableHashMapMemo[K, V](ttl: Option[FiniteDuration], GCtriggerMill: Option[FiniteDuration]): Memo[K, V] =
    immutableMapMemo(new immutable.HashMap[K, Value[V]], ttl, GCtriggerMill)

  /** Cache results in a list map.  Nonsensical unless `K` has
    * a meaningful `java.lang.Object.equals`.  $immuMapNote
    */
  def immutableListMapMemo[K, V](ttl: Option[FiniteDuration], GCtriggerMill: Option[FiniteDuration]): Memo[K, V] =
    immutableMapMemo(new immutable.ListMap[K, Value[V]], ttl, GCtriggerMill)

  /** Cache results in a tree map. $immuMapNote */
  def immutableTreeMapMemo[K: scala.Ordering, V](
    ttl: Option[FiniteDuration],
    timeToCleanMill: Option[FiniteDuration]
  ): Memo[K, V] =
    immutableMapMemo(new immutable.TreeMap[K, Value[V]], ttl, timeToCleanMill)
}
