package fr.ramiro.zio.jmxagent.influxdb

import java.lang.management.ManagementFactory
import java.time.ZonedDateTime
import java.util

import javax.management.openmbean.{CompositeData, TabularData}
import javax.management.{Attribute, ObjectInstance, ObjectName}
import zio.{IO, Task}
import zio.syntax._

import scala.jdk.CollectionConverters._
import scala.util.Try

case class MBeanResultRaw(
    oi: ObjectInstance,
    attrs: List[Attribute],
    timestamp: ZonedDateTime = ZonedDateTime.now()
)
sealed trait MBeanValue
case class MBeanNumber(value: Number) extends MBeanValue
case class MBeanString(value: String) extends MBeanValue
case class MBeanAttribute(valuePath: List[String], valueAny: MBeanValue)

case class MBeanResult(
    oi: String,
    attrs: List[MBeanAttribute],
    timestamp: ZonedDateTime
)

object PlatformMBean {
  private lazy val mbeanServer = Try(ManagementFactory.getPlatformMBeanServer)

  private def queryNames(objectName: String) =
    IO.fromTry(mbeanServer) >>= (
        mbs =>
          IO.effect(mbs.queryNames(new ObjectName(objectName), null).asScala)
      )
  private def getObjectInstance(objectName: ObjectName) =
    IO.fromTry(mbeanServer) >>= (
        mbs => IO.effect(mbs.getObjectInstance(objectName))
    )
  private def getAttributeNames(objectName: ObjectName) =
    IO.fromTry(mbeanServer) >>= (
        mbs =>
          IO.effect(
            mbs.getMBeanInfo(objectName).getAttributes.toList.map(_.getName)
          )
      )
  private def getAttributes(objectName: ObjectName)(attributes: List[String]) =
    IO.fromTry(mbeanServer) >>= (
        mbs =>
          IO.effect(
            mbs
              .getAttributes(objectName, attributes.toArray)
              .asList()
              .asScala
              .toList
          )
      )

  private def getObjectAttributes(
      objectName: ObjectName,
      attributes: List[String]
  ) =
    (if (attributes.isEmpty) getAttributeNames(objectName)
     else IO.succeed(attributes)) >>= getAttributes(objectName)

  def collect(
      objectName: String,
      attributes: List[String]
  ): Task[List[MBeanResultRaw]] =
    queryNames(objectName) >>= (
        names => IO.collectAllPar(names.map(collectObject(attributes)))
    )

  def collectObject(
      attributes: List[String]
  )(objectName: ObjectName): Task[MBeanResultRaw] = {
    (getObjectInstance(objectName), getObjectAttributes(objectName, attributes))
      .map2(MBeanResultRaw(_, _))
  }

  def jmxResultProcessor(mBeanQueryResult: MBeanResultRaw): MBeanResult =
    MBeanResult(
      mBeanQueryResult.oi.getObjectName.getKeyPropertyListString,
      mBeanQueryResult.attrs.flatMap(
        attribute => decompose(List(attribute.getName), attribute.getValue)
      ),
      mBeanQueryResult.timestamp
    )

  def decompose(valuePath: List[String], valueAny: Any): List[MBeanAttribute] =
    valueAny match {
      case null => List.empty
      case cds: CompositeData =>
        cds.getCompositeType.keySet.asScala.toList
          .flatMap(key => decompose(valuePath :+ key, cds.get(key)))
      case cdsArray: Array[CompositeData] =>
        cdsArray.toList.flatMap(cd => decompose(List.empty, cd))
      case objs: Array[ObjectName] =>
        objs.toList.flatMap(
          obj =>
            decompose(
              valuePath :+ obj.getCanonicalName,
              obj.getKeyPropertyListString
            )
        )
      case array: Array[_] =>
        array.zipWithIndex.toList.flatMap {
          case (value, index) => decompose(valuePath :+ index.toString, value)
        }
      case tds: TabularData =>
        tds
          .keySet()
          .asInstanceOf[util.Set[util.List[String]]]
          .asScala
          .toList
          .map(_.asScala)
          .flatMap(
            key =>
              decompose(valuePath :+ key.mkString("."), tds.get(key.toArray))
          )
      case map: util.Map[_, _] =>
        map.asScala.toList.flatMap {
          case (key, value) => decompose(valuePath :+ key.toString, value)
        }
      case iterable: java.lang.Iterable[_] =>
        iterable.asScala.zipWithIndex.toList.flatMap {
          case (value, index) => decompose(valuePath :+ index.toString, value)
        }
      case value: Number => List(MBeanAttribute(valuePath, MBeanNumber(value)))
      //TODO influx boolean
      case value => List(MBeanAttribute(valuePath, MBeanString(value.toString)))
    }
}
