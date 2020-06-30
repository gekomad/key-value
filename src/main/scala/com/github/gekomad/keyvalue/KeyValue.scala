package com.github.gekomad.keyvalue

import scala.concurrent.Future
import scala.concurrent.ExecutionContext
import java.util.concurrent.Executors
import scala.concurrent.duration._

trait KeyValue[A, B] {
  private implicit val ec = ExecutionContext.fromExecutorService(Executors.newWorkStealingPool(1))

  private class Value(val value: B, val ttl: Option[Long]) {
    val created: Long = System.currentTimeMillis()
  }

  protected val _GCtriggerMill: Long
  private val map: scala.collection.mutable.Map[String, scala.collection.mutable.Map[A, Value]] =
    scala.collection.mutable.Map.empty

  Future(while (true) {

    Thread.sleep(_GCtriggerMill)

    for {
      (key1, subMap) <- map
      (id, theValue) <- subMap
    } yield {
      theValue.ttl match {
        case Some(time) =>
          if ((System.currentTimeMillis() - theValue.created) > time) delete(key1, id)
        case None => ()
      }
    }

  })

  def set(key1: String, key2: A, value: B, ttl: Option[Long] = None): Unit = map.get(key1) match {
    case Some(g) => g += (key2   -> new Value(value, ttl))
    case None    => map += (key1 -> scala.collection.mutable.Map(key2 -> new Value(value, ttl)))
  }

  def clear(): Unit = map.clear()

  def get(key1: String, key2: A): Option[B] = map.get(key1) match {
    case Some(g) =>
      g.get(key2).fold(None: Option[B]) { theValue =>
        theValue.ttl match {
          case Some(time) =>
            if ((System.currentTimeMillis() - theValue.created) > time) {
              delete(key1, key2)
              None
            } else Some(theValue.value)
          case None => Some(theValue.value)
        }

      }
    case None => None
  }

  def clear(key1: String): Unit = map -= key1

  def delete(key1: String, key2: A): Unit = map.get(key1) match {
    case Some(g) =>
      g -= key2
      if (g.isEmpty) map -= key1
    case _ => ()
  }

  def size: Int = map.size

  def size(key1: String): Int = map.get(key1) match {
    case Some(g) => g.size
    case None    => 0
  }

}

object KeyValue {

  def apply[A, B](GCtriggerMill: Long = 1.hour.toMillis): KeyValue[A, B] = new KeyValue[A, B] {
    override val _GCtriggerMill: Long = GCtriggerMill
  }

}
