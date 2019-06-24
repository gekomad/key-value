/**
  * Scalaz Memo with TTL
  */
package com.github.gekomad.keyvalue

import java.util.concurrent.Executors
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}

class Value[@specialized(Int, Long, Double) V](val value: V, val ttl: Option[Long]) {
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
  def memo[@specialized(Int) K, @specialized(Int, Long, Double) V](f: (K => V) => K => V): Memo[K, V] =
    new Memo[K, V] {
      override def apply(z: K => V): K => V = {
        val a = f(z)
        a
      }
    }

  def nilMemo[@specialized(Int) K, @specialized(Int, Long, Double) V]: Memo[K, V] =
    memo[K, V](f => k => new Value(f(k), None).value)

  import collection.mutable

  def mutableMapMemo[K, V](a: mutable.Map[K, V]): Memo[K, V] =
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

  def immutableMapMemo[K, V](m: immutable.Map[K, Value[V]], ttl: Option[Long], timeToCleanMill: Long): Memo[K, V] = {
    var map = m

    implicit val ec = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(1))

    Future(while (true) {

      Thread.sleep(timeToCleanMill)

      for {
        (idx1, theValue) <- map

      } yield {
        theValue.ttl match {
          case Some(time) =>
            if ((System.currentTimeMillis() - theValue.created) > time) map -= idx1
          case None => ()
        }
      }

    })

    memo[K, V](
      f =>
        k => {
          val optValue = map get k
          val value = optValue.getOrElse {
            val x = new Value(f(k), ttl)
            map = map updated (k, x)
            x
          }
          value.ttl match {
            case Some(time) =>
              if ((System.currentTimeMillis() - value.created) > time) {
                val x = new Value(f(k), ttl)
                map = map updated (k, x)
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
  def immutableHashMapMemo[K, V](ttl: Option[Long], timeToCleanMill: Long = 1.hour.toMillis): Memo[K, V] = //TODO >0
    immutableMapMemo(new immutable.HashMap[K, Value[V]], ttl, timeToCleanMill)

  /** Cache results in a list map.  Nonsensical unless `K` has
    * a meaningful `java.lang.Object.equals`.  $immuMapNote
    */
  def immutableListMapMemo[K, V](ttl: Option[Long], timeToCleanMill: Long = 1.hour.toMillis): Memo[K, V] =
    immutableMapMemo(new immutable.ListMap[K, Value[V]], ttl, timeToCleanMill)

  /** Cache results in a tree map. $immuMapNote */
  def immutableTreeMapMemo[K: scala.Ordering, V](
    ttl: Option[Long],
    timeToCleanMill: Long = 1.hour.toMillis
  ): Memo[K, V] =
    immutableMapMemo(new immutable.TreeMap[K, Value[V]], ttl, timeToCleanMill)
}
