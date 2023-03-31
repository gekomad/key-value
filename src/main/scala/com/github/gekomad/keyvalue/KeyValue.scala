package com.github.gekomad.keyvalue

import scala.concurrent.{ExecutionContext, ExecutionContextExecutorService, Future}
import java.util.concurrent.Executors

import scala.concurrent.duration._

final case class KeyValue[K, V](GCtriggerMill: Option[FiniteDuration]) {
  private implicit val ec: ExecutionContextExecutorService =
    ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(1))

  private class Value(val value: V, val ttl: Option[Long]) {
    val created: Long = System.currentTimeMillis()
  }

  private val map: scala.collection.mutable.Map[K, Value] = scala.collection.mutable.Map.empty[K, Value]

  def gc(): Unit = map.foreach {
    case (key, value) =>
      value.ttl match {
        case Some(time) => if ((System.currentTimeMillis() - value.created) > time) delete(key)
        case None       => ()
      }
  }

  GCtriggerMill.foreach { d =>
    Future(while (true) {
      Thread.sleep(d.toMillis)
      gc()
    })
  }

  def set(key: K, value: V, ttl: Option[FiniteDuration] = None): Unit =
    map += (key -> new Value(value, ttl.map(_.toMillis)))

  def clear(): Unit = map.clear()

  def get(key: K): Option[V] = map.get(key) match {
    case None => None
    case Some(value) =>
      value.ttl match {
        case Some(time) =>
          if ((System.currentTimeMillis() - value.created) > time) {
            delete(key)
            None
          } else
            Some(value.value)
        case None => Some(value.value)
      }
  }

  def delete(key: K): Unit = map -= key

  def size: Int = map.size
}
