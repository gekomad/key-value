package com.github.gekomad.keyvalue

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global

trait Cleaner {
  def GC(): Unit
  def invalidate(): Unit
}

object KeyValue {
  private val list: scala.collection.mutable.ListBuffer[Cleaner] = scala.collection.mutable.ListBuffer.empty
  def add[K, V](GCtriggerMill: Option[FiniteDuration], mainTLL: Option[FiniteDuration]): keyValue[K, V] = {
    val x = new keyValue[K, V](GCtriggerMill = GCtriggerMill, mainTLL = mainTLL)
    list.addOne(x)
    x
  }

  def meomizeFunc[K, V](kv: keyValue[K, V])(f: K => V): K => V =
    (k: K) =>
      kv.get(k) match {
        case Some(value) => value
        case None =>
          val res = f(k)
          kv.set(k, res)
          res
    }

  def GC(): Unit         = list.foreach(_.GC())
  def invalidate(): Unit = list.foreach(_.invalidate())

  class keyValue[K, V](GCtriggerMill: Option[FiniteDuration], mainTLL: Option[FiniteDuration]) extends Cleaner {

    private class Value(val value: V, val ttl: Option[FiniteDuration]) {
      val created: Long = System.currentTimeMillis()
    }

    private val map: scala.collection.mutable.Map[K, Value] = scala.collection.mutable.Map.empty[K, Value]

    def GC(): Unit = map.foreach {
      case (key, value) =>
        value.ttl match {
          case Some(time) => if ((System.currentTimeMillis() - value.created) > time.toMillis) delete(key)
          case None       => ()
        }
    }

    GCtriggerMill.foreach { d =>
      Future(while (true) {
        Thread.sleep(d.toMillis)
        GC()
      })
    }

    def set(key: K, value: V, ttl: Option[FiniteDuration] = mainTLL): Unit =
      map += (key -> new Value(value, ttl))

    def invalidate(): Unit = map.clear()

    def get(key: K): Option[V] = getValueAndTime(key).map(_._1)

    def getValueAndTime(key: K): Option[(V, Long)] = map.get(key) match {
      case None => None
      case Some(value) =>
        value.ttl match {
          case Some(time) =>
            if ((System.currentTimeMillis() - value.created) > time.toMillis) {
              delete(key)
              None
            } else Some((value.value, value.created))
          case None => Some((value.value, value.created))
        }
    }

    def delete(key: K): Unit = map -= key

    def size: Int = map.size
  }
}
