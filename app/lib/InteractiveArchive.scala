package lib

import java.nio.file.{Files, Path}
import java.util.Locale
import java.util.zip.ZipInputStream
import scala.collection.mutable
import scala.jdk.CollectionConverters._

case class InteractiveAsset(path: Path, relativePath: String, contentType: String)
case class InvalidInteractive(message: String) extends IllegalArgumentException(message)

object InteractiveArchive {
  val maxFiles = 2000
  val maxFileBytes: Long = 50L * 1024 * 1024
  val maxExpandedBytes: Long = 200L * 1024 * 1024

  private val contentTypes = Map(
    "html" -> "text/html; charset=utf-8", "htm" -> "text/html; charset=utf-8",
    "css" -> "text/css; charset=utf-8", "js" -> "text/javascript; charset=utf-8",
    "mjs" -> "text/javascript; charset=utf-8", "json" -> "application/json",
    "map" -> "application/json", "geojson" -> "application/geo+json",
    "csv" -> "text/csv; charset=utf-8", "tsv" -> "text/tab-separated-values; charset=utf-8",
    "txt" -> "text/plain; charset=utf-8", "xml" -> "application/xml",
    "svg" -> "image/svg+xml", "png" -> "image/png", "jpg" -> "image/jpeg",
    "jpeg" -> "image/jpeg", "gif" -> "image/gif", "webp" -> "image/webp",
    "avif" -> "image/avif", "ico" -> "image/x-icon", "woff" -> "font/woff",
    "woff2" -> "font/woff2", "ttf" -> "font/ttf", "otf" -> "font/otf",
    "mp4" -> "video/mp4", "webm" -> "video/webm", "mp3" -> "audio/mpeg",
    "ogg" -> "audio/ogg", "wasm" -> "application/wasm", "pdf" -> "application/pdf"
  )

  def projectSlug(name: String): String = {
    val slug = name.toLowerCase(Locale.ROOT).trim.stripSuffix(".zip")
      .replaceAll("[^a-z0-9_-]+", "-").stripPrefix("-").stripSuffix("-").take(80)
    if (slug.isEmpty) "visual" else slug
  }

  def withAssets[A](archive: Path)(publish: Seq[InteractiveAsset] => A): A = {
    val directory = Files.createTempDirectory("interactive-upload-")
    try {
      val zip = new ZipInputStream(Files.newInputStream(archive))
      val assets = mutable.ArrayBuffer.empty[InteractiveAsset]
      val seen = mutable.Set.empty[String]
      var totalBytes = 0L
      var entries = 0
      try {
        var entry = zip.getNextEntry
        val buffer = new Array[Byte](8192)
        while (entry != null) {
          entries += 1
          if (entries > maxFiles) throw InvalidInteractive(s"ZIP must contain at most $maxFiles entries.")
          val name = entry.getName
          val segments = name.split("/", -1)
          if (name.isEmpty || name.startsWith("/") || name.contains("\\") || name.contains(":") ||
              name.exists(c => Character.isISOControl(c)) || segments.contains("..") || segments.contains(".") || segments.dropRight(1).contains("")) {
            throw InvalidInteractive("ZIP contains an unsafe file path.")
          }
          val destination = directory.resolve(name).normalize()
          if (!destination.startsWith(directory)) throw InvalidInteractive("ZIP contains an unsafe file path.")
          val ignored = segments.contains("__MACOSX") || segments.contains(".DS_Store") ||
            segments.exists(_.startsWith("._"))
          if (!entry.isDirectory && !ignored) {
            if (!seen.add(name)) throw InvalidInteractive("ZIP contains duplicate file paths.")
            Files.createDirectories(destination.getParent)
            val output = Files.newOutputStream(destination)
            try {
              var fileBytes = 0L
              var count = zip.read(buffer)
              while (count != -1) {
                fileBytes += count
                totalBytes += count
                if (fileBytes > maxFileBytes || totalBytes > maxExpandedBytes) {
                  throw InvalidInteractive("Expanded ZIP exceeds the 50 MB per file or 200 MB total limit.")
                }
                output.write(buffer, 0, count)
                count = zip.read(buffer)
              }
            } finally output.close()
            val extension = name.split('.').last.toLowerCase(Locale.ROOT)
            assets += InteractiveAsset(destination, name, contentTypes.getOrElse(extension, "application/octet-stream"))
          } else {
            // Count discarded content too, so ignored entries cannot bypass expansion limits.
            var fileBytes = 0L
            var count = zip.read(buffer)
            while (count != -1) {
              fileBytes += count
              totalBytes += count
              if (fileBytes > maxFileBytes || totalBytes > maxExpandedBytes) {
                throw InvalidInteractive("Expanded ZIP exceeds the 50 MB per file or 200 MB total limit.")
              }
              count = zip.read(buffer)
            }
          }
          entry = zip.getNextEntry
        }
      } catch {
        case _: java.util.zip.ZipException | _: java.io.EOFException =>
          throw InvalidInteractive("ZIP is corrupt or incomplete.")
        case _: java.nio.file.FileAlreadyExistsException | _: java.nio.file.NotDirectoryException =>
          throw InvalidInteractive("ZIP contains conflicting file and folder paths.")
      } finally zip.close()
      if (!assets.exists(_.relativePath == "index.html")) {
        throw InvalidInteractive("ZIP must contain index.html at its root. Zip the folder's contents, rather than the folder itself.")
      }
      publish(assets.toSeq)
    } finally {
      val paths = Files.walk(directory)
      try paths.iterator().asScala.toSeq.sortBy(_.getNameCount).reverse.foreach(Files.deleteIfExists)
      finally paths.close()
    }
  }
}
