package lib

import java.net.URI
import java.nio.file.Path
import java.time.{LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter
import java.util.UUID
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.model.{DeleteObjectRequest, ObjectCannedACL, PutObjectRequest}
import scala.collection.mutable
import scala.jdk.CollectionConverters._

case class PublishedInteractive(url: String, files: Seq[(String, String)])

class InteractivePublisher(config: Config) {
  private def publicUrl(key: String): String = {
    val base = config.maybePrettyBaseUrl.getOrElse(s"https://${config.bucketName}.s3.${config.region}.amazonaws.com")
    base.stripSuffix("/") + new URI(null, null, "/" + key, null).toASCIIString
  }

  def publish(archive: Path, project: String, authorEmail: String): PublishedInteractive = {
    InteractiveArchive.withAssets(archive) { assets =>
      val date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy/MM"))
      val prefix = config.key(s"$date/${InteractiveArchive.projectSlug(project)}/${UUID.randomUUID()}")
      val uploaded = mutable.ArrayBuffer.empty[String]
      try {
        // Publish the entry point last, after all of its assets are available.
        assets.sortBy(_.relativePath == "index.html").foreach { asset =>
          val key = s"$prefix/${asset.relativePath}"
          val request = PutObjectRequest.builder().bucket(config.bucketName).key(key)
            .contentType(asset.contentType).cacheControl("max-age=604800")
            .acl(ObjectCannedACL.PUBLIC_READ).metadata(Map("author" -> authorEmail).asJava).build()
          config.s3Client.putObject(request, RequestBody.fromFile(asset.path))
          uploaded += key
        }
        PublishedInteractive(publicUrl(s"$prefix/index.html"), assets.map { asset =>
          asset.relativePath -> publicUrl(s"$prefix/${asset.relativePath}")
        })
      } catch {
        case e: Exception =>
          uploaded.foreach { key =>
            try config.s3Client.deleteObject(DeleteObjectRequest.builder().bucket(config.bucketName).key(key).build())
            catch { case cleanup: Exception => e.addSuppressed(cleanup) }
          }
          throw e
      }
    }
  }
}
