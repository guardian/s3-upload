package lib

import java.io.File
import java.net.URI
import java.text.SimpleDateFormat
import java.util.Calendar

import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.services.s3.model._
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

class S3Actions() {
  private val s3NotFoundStatusList = List(403, 404)

  def upload(file: File, user: User, bucketName: String, s3Client: AmazonS3, key: String, setPublicAcl: Boolean): S3UploadResponse = {

    getObject(key, bucketName, s3Client) match {
      case None => {
        val metadata = new ObjectMetadata
        metadata.addUserMetadata("author", user.email)
        if(setPublicAcl) { metadata.setContentType("text/html") }

        val request = new PutObjectRequest(bucketName, key, file).withMetadata(metadata)

        val finalRequest = if(setPublicAcl) {
          request.withCannedAcl(CannedAccessControlList.PublicRead)
        } else { request }

        s3Client.putObject(finalRequest) match {
          case _: PutObjectResult => S3UploadResponse.buildSuccess(finalRequest)
          case _ => S3UploadFailure(None, None, Some(file.getName), Some("Upload Failed"))
        }

      }
      case _ => S3UploadFailure(None, None, Some(file.getName), Some("File with that name already exists"))
    }
  }

  def getObject(key: String, bucketName: String, s3Client: AmazonS3) = {

    try {
      s3Client.getObject(bucketName, key)
    } catch {
      case e: AmazonS3Exception if s3NotFoundStatusList.contains(e.getStatusCode) => None
    }
  }




}

