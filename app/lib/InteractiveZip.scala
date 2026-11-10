package lib

import java.time.format.DateTimeFormatter
import java.time.{LocalDate, ZoneOffset}
import java.util.{Date, Locale}
import java.util.zip.{ZipEntry, ZipException, ZipFile}
import scala.jdk.CollectionConverters._
import scala.util.Using

object InteractiveZip {
  // note - maximum overall zip size of 50mb is enforced by play.http.parser.maxDiskBuffer in application.conf
  val MAX_FILES_IN_ZIP: Int = 2000
  val MAX_FILE_SIZE: Int = 50 * 1024 * 1024 // 50mb
  val MAX_EXPANDED_ZIP_SIZE: Long = 200L * 1024 * 1024 // 200mb
  val CACHE_CONTROL_MAX_AGE = 604800 // 7 days
  val MAX_SLUG_LENGTH = 80
  val INTERACTIVE_ZIP_INPUT_NAME = "zipfile" // must match what's used in interactive.scala.html
  val INTERACTIVE_PROJECT_NAME_INPUT_NAME = "projectName" // must match what's used in interactive.scala.html

  private val dateToday = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
  private val DEFAULT_SLUG = s"interactive-upload-$dateToday"

  def projectSlug(name: String): String = {
    val slug = name.toLowerCase(Locale.ROOT).trim.stripSuffix(".zip")
      .replaceAll("[^a-z0-9_-]+", "-").stripPrefix("-").stripSuffix("-").take(MAX_SLUG_LENGTH)
    if (slug.isEmpty) DEFAULT_SLUG else slug
  }

  def containsUnsafeChars(path: String): Boolean = {
    path.startsWith("/") || // directory
    path.contains("\\") || // backslashes
    path.contains(":") || // windows drive paths
    path.exists(Character.isISOControl) || // newlines/tabs/null chars
    path.split("/", -1).exists(part => part.isEmpty || part == "." || part == "..") // path traversal
  }

  // get all the paths in the zip with various bits of validation
  def getZippedFiles(zip: ZipFile): Vector[ZipEntry] = {
    val entries = zip.entries().asScala.take(MAX_FILES_IN_ZIP + 1).toVector
    if (entries.size > MAX_FILES_IN_ZIP) throw new ZipException(s"ZIP must contain at most $MAX_FILES_IN_ZIP entries.")
    // discard macOS artifacts/directories
    val files = entries.filterNot(entry => entry.isDirectory || entry.getName.split("/").exists { part =>
      part == "__MACOSX" || part == ".DS_Store" || part.startsWith("._")
    })
    val filePaths = files.map(_.getName)
    if (!filePaths.contains("index.html")) throw new ZipException("ZIP must contain index.html at its root.")
    if (filePaths.distinct.size != filePaths.size) throw new ZipException("ZIP contains duplicate file paths.")
    filePaths.foreach { path =>
      if (containsUnsafeChars(path)) {
        throw new ZipException("ZIP contains an unsafe file path.")
      }
    }
    if (files.exists(entry => entry.getSize < 0 || entry.getSize > MAX_FILE_SIZE) || files.map(_.getSize).sum > MAX_EXPANDED_ZIP_SIZE) {
      throw new ZipException("Expanded ZIP exceeds the 50 MB per file or 200 MB total limit.")
    }
    files
  }

  def readFileFromZip(zip: ZipFile, entry: ZipEntry): Array[Byte] = {
    // Bound actual decompression too: ZIP size metadata cannot be trusted.
    val bytes = Using.resource(zip.getInputStream(entry))(_.readNBytes(MAX_FILE_SIZE + 1))
    if (bytes.length > MAX_FILE_SIZE || bytes.length.toLong != entry.getSize) throw new ZipException("ZIP has invalid file sizes.")
    bytes
  }
}
