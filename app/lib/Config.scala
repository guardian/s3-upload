package lib

import java.io.FileNotFoundException
import java.net.URI

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}

import scala.io.Source
import scala.util.Try

object Config {
  val properties: Map[String, String] = Try(Properties.fromPath("/etc/gu/s3-uploader.properties")).getOrElse(Map.empty)

  val awsCredentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("media-service"),
    InstanceProfileCredentialsProvider.getInstance()
  )

  val region = properties.getOrElse("aws.region", "eu-west-1")

  val bucketName = properties.getOrElse("s3.bucket", "s3-uploader-dev-bucket")

  val domain = properties.getOrElse("panda.domain", "local.dev-gutools.co.uk")

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
