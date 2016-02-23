package lib

import java.io.FileNotFoundException
import java.net.URI
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import scala.io.Source

object Config {
  val properties = Properties.fromPath("/etc/gu/s3-uploader.properties")

  val awsCredentials = new DefaultAWSCredentialsProviderChain()

  val bucketName = properties("s3.bucket")

  val domain = properties("panda.domain")

  val maxContentLength = properties.getOrElse("max.content.length", "50").toLong

  val maxContentLengthMB = maxContentLength * 1024 * 1024

  val loginUri = new URI(s"https://login.$domain/login?returnUrl=https://s3-uploader.$domain")

  implicit val stage : String = {
    try {
      val stageFile = Source.fromFile("/etc/gu/stage")
      val stage = stageFile.getLines.next
      stageFile.close()
      if (List("PROD", "CODE").contains(stage)) stage else "DEV"
    }
    catch {
      case e: FileNotFoundException => "DEV"
    }
  }
}
