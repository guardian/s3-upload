package lib

import java.io.File
import java.net.URI
import java.util.{Locale, UUID}
import java.nio.file.Path
import java.util.zip.ZipFile
import play.api.http.FileMimeTypes
import scala.util.Using
import software.amazon.awssdk.services.s3.S3Client
import com.gu.pandomainauth.model.User
import play.api.libs.json.{JsPath, JsString, Writes}
import play.api.libs.functional.syntax._
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model._

import scala.jdk.CollectionConverters._

trait S3UploadResponse {
  def url: Option[URI]
  def fileName : Option[String]
  def msg  : Option[String]
}

case class S3UploadSuccess(url: Option[URI], fileName: Option[String], msg: Option[String])
  extends S3UploadResponse

case class S3UploadFailure(url: Option[URI], fileName: Option[String], msg: Option[String])
  extends S3UploadResponse

object S3UploadResponse {
  def buildSuccess(putObjectRequest: PutObjectRequest, config: Config) = {
    val uri = config.maybePrettyBaseUrl match {
      case Some(prettyBaseUrl) =>
        s"$prettyBaseUrl/${putObjectRequest.key()}"
      case None =>
        s"https://s3-eu-west-1.amazonaws.com/${putObjectRequest.bucket()}/${putObjectRequest.key()}"
    }

    S3UploadSuccess(Some(new URI(uri)), Some(putObjectRequest.key()), None)
  }

  implicit val writesUri: Writes[URI] = (uri: URI) => JsString(uri.toString)

  implicit val s3UploadSuccessWrites: Writes[S3UploadSuccess] = (
    (JsPath \ "url").writeNullable[URI] and
    (JsPath \ "fileName").writeNullable[String] and
    (JsPath \ "msg").writeNullable[String]
  )(unlift(S3UploadSuccess.unapply))

  implicit val s3UploadFailureWrites: Writes[S3UploadFailure] = (failure: S3UploadFailure) => JsString(failure.msg.getOrElse("Something went wrong"))
}

class S3Actions() {
  private val s3NotFoundStatusList = List(403, 404)
  private val imageContentTypes = Map(
    "jpg" -> "image/jpeg",
    "jpeg" -> "image/jpeg",
    "png" -> "image/png",
    "gif" -> "image/gif",
    "webp" -> "image/webp"
  )

  def upload(file: File, fileName: String, user: User, config: Config, setPublicAcl: Boolean): S3UploadResponse = {

    getObject(config.key(fileName), config.bucketName, config.s3Client) match {
      case None => {
        val metadata = Map("author" -> user.email)
        val requestBuilder = PutObjectRequest.builder().bucket(config.bucketName).key(config.key(fileName)).metadata(metadata.asJava)

        if (setPublicAcl) {
          requestBuilder.contentType("text/html").acl(ObjectCannedACL.PUBLIC_READ)
        } else {
          imageContentType(fileName).foreach(requestBuilder.contentType)
        }
        val request = requestBuilder.build()

        config.s3Client.putObject(request, RequestBody.fromFile(file.toPath)) match {
          case _: PutObjectResponse => S3UploadResponse.buildSuccess(request, config)
          case _ => S3UploadFailure(None, Some(fileName), Some("Upload Failed"))
        }

      }
      case _ => S3UploadFailure(None, Some(fileName), Some("File with that name already exists"))
    }
  }

  def uploadInteractive(archive: Path, project: String, author: String, config: Config, mimeTypes: FileMimeTypes): S3UploadSuccess = {
    Using.resource(new ZipFile(archive.toFile)) { zip =>
      val prefix = config.key(s"${InteractiveArchive.projectSlug(project)}/${UUID.randomUUID()}")
      // Publish index.html last, so its assets are available before we return its URL.
      val uploaded = InteractiveArchive.files(zip).sortBy(_.getName == "index.html").map { entry =>
        val request = PutObjectRequest.builder().bucket(config.bucketName).key(s"$prefix/${entry.getName}")
          .contentType(mimeTypes.forFileName(entry.getName).getOrElse("application/octet-stream"))
          .cacheControl("max-age=604800").acl(ObjectCannedACL.PUBLIC_READ)
          .metadata(Map("author" -> author).asJava).build()
        config.s3Client.putObject(request, RequestBody.fromBytes(InteractiveArchive.read(zip, entry)))
        request
      }
      S3UploadResponse.buildSuccess(uploaded.last, config)
    }
  }

  private def imageContentType(fileName: String): Option[String] = {
    val extensionStart = fileName.lastIndexOf('.')
    if (extensionStart < 0) None
    else imageContentTypes.get(fileName.substring(extensionStart + 1).toLowerCase(Locale.ROOT))
  }

  def getObject(key: String, bucketName: String, s3Client: S3Client) = {
    try {
      s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build())
    } catch {
      case e: S3Exception if s3NotFoundStatusList.contains(e.statusCode()) => None
    }
  }

}
