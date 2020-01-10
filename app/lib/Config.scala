package lib

import java.io.{File, FileNotFoundException}
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar

import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.auth.{AWSCredentialsProviderChain, InstanceProfileCredentialsProvider}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import scala.io.Source
import scala.util.Try

trait Config {
  // config properties required for each new app using the S3-Uploader
  val bucketName: String
  val region: String
  val s3Client: AmazonS3
  def key (fileName: String): String
  val prettyUrl: String

  val properties: Map[String, String] = Try(Properties.fromPath("/etc/gu/s3-uploader.properties")).getOrElse(Map.empty)

  val awsCredentials = new AWSCredentialsProviderChain(
    new ProfileCredentialsProvider("media-service"),
    InstanceProfileCredentialsProvider.getInstance()
  )

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


object S3UploadAppConfig extends Config {
  val bucketName = properties.getOrElse("s3.bucket", "s3-uploader-dev-bucket")
  val region = properties.getOrElse("aws.region", "eu-west-1")
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(awsCredentials).build()
  val prettyUrl = "https://uploads.guim.co.uk"

  def key(fileName: String) = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("yyyy/MM/dd")
    val getCurrentDate = dateFormat.format(now)
    s"$getCurrentDate/${fileName.replace(' ', '_')}"
  }
}


object ChartsToolConfig extends Config {
  val bucketName = properties.getOrElse("s3.chartBucket", "gdn-cdn")
  val region = properties.getOrElse("aws.chartRegion", "us-east-1")
  val s3Client = AmazonS3ClientBuilder.standard().withRegion(region).withCredentials(awsCredentials).build()
  val prettyUrl = "https://interactive.guim.co.uk"

  def key(fileName: String) = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("MMM/yyyy-MM-dd-hh:mm:ss")
    s"charts/embed/${dateFormat.format(now).toLowerCase}/embed.html"
  }
}