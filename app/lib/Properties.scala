package lib

import java.io.{InputStream, FileInputStream}

import scala.jdk.CollectionConverters._


object Properties {

  def fromPath(file: String): Map[String, String] =
    fromStream(new FileInputStream(file))

  def fromStream(stream: InputStream): Map[String, String] = {
    val props = new java.util.Properties
    try props.load(stream) finally stream.close()
    props.asScala.toMap
  }
}
