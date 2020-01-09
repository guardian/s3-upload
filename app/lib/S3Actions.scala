package lib

import java.io.File
import java.net.URI

import com.amazonaws.services.s3.AmazonS3
import com.amazonaws.services.s3.model._
import com.gu.pandomainauth.model.User
import play.api.libs.json.{JsPath, JsString, JsValue, Writes}
import play.api.libs.functional.syntax._

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
  def buildSuccess(putObjectRequest: PutObjectRequest, config: Config) = {
    val directToS3Uri = s"https://s3-eu-west-1.amazonaws.com/${putObjectRequest.getBucketName}/${putObjectRequest.getKey}"

    val uri = config.stage match {
      case "PROD" => s"${config.prettyUrl}/${putObjectRequest.getKey}"
      case _ => directToS3Uri
    }
    
    S3UploadSuccess(Some(new URI(uri)), Some(new URI(directToS3Uri)), Some(putObjectRequest.getKey), None)
  }

  implicit val writesUri: Writes[URI] = (uri: URI) => JsString(uri.toString)

  implicit val s3UploadSuccessWrites: Writes[S3UploadSuccess] = (
    (JsPath \ "url").writeNullable[URI] and
    (JsPath \ "directToS3Url").writeNullable[URI] and
    (JsPath \ "fileName").writeNullable[String] and
    (JsPath \ "msg").writeNullable[String]
  )(unlift(S3UploadSuccess.unapply))

  implicit val s3UploadFailureWrites: Writes[S3UploadFailure] = (failure: S3UploadFailure) => JsString(failure.msg.getOrElse("Something went wrong"))
}

class S3Actions() {
  private val s3NotFoundStatusList = List(403, 404)

  def upload(file: File, user: User, config: Config, setPublicAcl: Boolean): S3UploadResponse = {

    getObject(config.key(file.getName), config.bucketName, config.s3Client) match {
      case None => {
        val metadata = new ObjectMetadata
        metadata.addUserMetadata("author", user.email)
        if(setPublicAcl) { metadata.setContentType("text/html") }

        val request = new PutObjectRequest(config.bucketName, config.key(file.getName), file).withMetadata(metadata)

        val finalRequest = if(setPublicAcl) {
          request.withCannedAcl(CannedAccessControlList.PublicRead)
        } else { request }

        config.s3Client.putObject(finalRequest) match {
          case _: PutObjectResult => S3UploadResponse.buildSuccess(finalRequest, config)
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

