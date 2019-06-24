//package com.github.gekomad.keyvalue
//
//import org.scalameter.{Key, Warmer, config}
//import org.scalatest.{BeforeAndAfterAll, FunSuite}
//
//import scala.util.Random
//
//class MeterTest extends FunSuite with BeforeAndAfterAll {
//
//  def randomInt(n: Int): Int = Random.nextInt(n)
//
//  def randomString(size: Int): String =
//    scala.util.Random.alphanumeric.take(size).mkString
//
//  test("scalaMeter") {
//
//    def goMapIntStringImm(): Unit = {
//      val name = randomString(4)
//      val i = randomInt(100)
//      val s = randomInt(100)
//      val kv = KV[Int, Int]
//      kv.set(name, i, s)
//      assert(kv.get(name, i).contains(s))
//    }
//
//    val standardConfig = config(
//      Key.exec.minWarmupRuns -> 10,
//      Key.exec.maxWarmupRuns -> 2000,
//      Key.exec.benchRuns -> 10000,
//      Key.verbose -> false
//    ) withWarmer new Warmer.Default
//
//    val time = standardConfig measure {
//      goMapIntStringImm()
//    }
//
//    println(s"scalameter time: $time")
//
//  }
//}
