package lib

import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar

import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.{ObjectMetadata, PutObjectRequest, PutObjectResult, AmazonS3Exception}
import com.gu.pandomainauth.model.User

trait S3UploadResponse {
  def url: Option[URI]
  def directToS3Url: Option[URI]
  def fileName : Option[String]
  def msg  : Option[String]
}

case class S3UploadSuccess(url: Option[URI], directToS3Url: Option[URI], fileName: Option[String], msg: Option[String])
  extends S3UploadResponse

case class S3UploadFailure(url: Option[URI], directToS3Url: Option[URI], fileName: Option[String], msg: Option[String])
  extends S3UploadResponse

object S3UploadResponse {
  def buildSuccess(putObjectRequest: PutObjectRequest) = {
    val directToS3Uri = s"https://s3-eu-west-1.amazonaws.com/${putObjectRequest.getBucketName}/${putObjectRequest.getKey}"

    val uri = Config.stage match {
      case "PROD" => s"https://uploads.guim.co.uk/${putObjectRequest.getKey}"
      case _ => directToS3Uri
    }
    
    S3UploadSuccess(Some(new URI(uri)), Some(new URI(directToS3Uri)), Some(putObjectRequest.getKey), None)
  }
}

object S3Actions {
  private val s3NotFoundStatusList = List(403, 404)

  val s3Client = new AmazonS3Client(Config.awsCredentials)

  def upload(file: File, user: User): S3UploadResponse = {
    val key = getS3Key(file)

    getObject(key) match {
      case None => {
        val metadata = new ObjectMetadata
        metadata.addUserMetadata("author", user.email)

        val request = new PutObjectRequest(Config.bucketName, key, file).withMetadata(metadata)

        s3Client.putObject(request) match {
          case _: PutObjectResult   => S3UploadResponse.buildSuccess(request)
          case _                 => S3UploadFailure(None, None, Some(file.getName), Some("Upload Failed"))
        }
      }
      case _ => S3UploadFailure(None, None, Some(file.getName), Some("File with that name already exists"))
    }
  }

  def getObject (key: String) = {
    try {
      s3Client.getObject(Config.bucketName, key)
    } catch {
      case e: AmazonS3Exception if s3NotFoundStatusList.contains(e.getStatusCode) => None
    }
  }

  private def getCurrentDate = {
    val now = Calendar.getInstance().getTime
    val dateFormat = new SimpleDateFormat("yyyy/MM/dd")
    dateFormat.format(now)
  }

  private def getS3Key (file: File) = {
    s"$getCurrentDate/${file.getName.replace(' ', '_')}"
  }
}

