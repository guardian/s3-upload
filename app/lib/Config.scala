package lib

import java.io.{File, FileNotFoundException}
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.auth.credentials
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain
import software.amazon.awssdk.regions.{Region => AWSRegion}

import scala.io.Source
import scala.util.Try

trait Config {
  // config properties required for each new app using the S3-Uploader
  val bucketName: String
  val region: String
  val s3Client: S3Client
  def key (fileName: String): String
  val maybePrettyBaseUrl: Option[String]

  val properties: Map[String, String] = Try(Properties.fromPath("/etc/gu/s3-uploader.properties")).getOrElse(Map.empty)

  val ProfileName = "media-service"

  lazy val credentialsProvider: AwsCredentialsProviderChain =
    credentials.AwsCredentialsProviderChain
      .builder()
      .credentialsProviders(
        credentials.ProfileCredentialsProvider.create(ProfileName),
        credentials.InstanceProfileCredentialsProvider.builder().build(),
      )
      .build()

  val domain = properties.getOrElse("panda.domain", "local.dev-gutools.co.uk")

  val loginUri = new URI(s"https://login.$domain/login?returnUrl=https://s3-uploader.$domain")

  val pinboardLoaderUrl = new URI(s"https://pinboard.$domain/pinboard.loader.js")

  implicit val stage : String = {
    try {
      val stageFile = Source.fromFile("/etc/gu/stage")
      val stage = stageFile.getLines().next()
      stageFile.close()
      if (List("PROD", "CODE").contains(stage)) stage else "DEV"
    }
    catch {
      case e: FileNotFoundException => "DEV"
    }
  }

  val telemetryUrl =
    s"https://user-telemetry.$domain/guardian-tool-accessed?app=s3-upload&stage=$stage"

}


object S3UploadAppConfig extends Config {
  val bucketName = properties.getOrElse("s3.bucket", "s3-uploader-dev-bucket")
  val region = properties.getOrElse("aws.region", "eu-west-1")
  val s3Client: S3Client = S3Client
    .builder()
    .region(AWSRegion.of(region))
    .credentialsProvider(credentialsProvider)
    .build()
  val maybePrettyBaseUrl = properties.get("prettyBaseUrl")

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
  val s3Client: S3Client = S3Client
    .builder()
    .region(AWSRegion.of(region))
    .credentialsProvider(credentialsProvider)
    .build()
  val maybePrettyBaseUrl = if (stage == "PROD") Some("https://interactive.guim.co.uk") else None

  def key(fileName: String) = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("MMM/yyyy-MM-dd-hh:mm:ss")
    s"charts/embed/${dateFormat.format(now).toLowerCase}/embed.html"
  }
}
