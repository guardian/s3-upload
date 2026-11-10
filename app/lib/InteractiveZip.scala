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

  def bytesToMb(bytes: Long): Long = bytes / 1024 / 1024

  sealed trait ZipError { def msg: String }
  case object TooManyFilesError extends ZipError {
    override def msg: String = s"ZIP must contain at most $MAX_FILES_IN_ZIP entries."
  }
  case object MissingIndexHtmlError extends ZipError {
    override def msg: String = "ZIP must contain index.html at its root."
  }
  case object DuplicateFilePathsError extends ZipError {
    override def msg: String = "ZIP contains duplicate file paths."
  }
  case object UnsafeFilePathError extends ZipError {
    override def msg: String = "Zip contains file or folder names with invalid characters"
  }
  case object OverallZipTooLargeError extends ZipError {
    override def msg: String = s"ZIP exceeds the ${bytesToMb(MAX_FILE_SIZE)} MB per file or ${bytesToMb(MAX_EXPANDED_ZIP_SIZE)} MB total limit."
  }

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

  def validateZip(zipEntries: List[ZipEntry]): Either[ZipError, List[ZipEntry]] = {
    // discard macOS artifacts/directories
    val files = zipEntries.filterNot(entry => entry.isDirectory || entry.getName.split("/").exists { part =>
      part == "__MACOSX" || part == ".DS_Store" || part.startsWith("._")
    })
    val filePaths = files.map(_.getName)

    if (zipEntries.size > MAX_FILES_IN_ZIP) Left(TooManyFilesError)
    else if (!filePaths.contains("index.html")) Left(MissingIndexHtmlError)
    else if (filePaths.distinct.size != filePaths.size) Left(DuplicateFilePathsError)
    else if (filePaths.exists(containsUnsafeChars)) Left(UnsafeFilePathError)
    else if (files.exists(entry => entry.getSize < 0 || entry.getSize > MAX_FILE_SIZE) || files.map(_.getSize).sum > MAX_EXPANDED_ZIP_SIZE) {
      Left(OverallZipTooLargeError)
    }
    else Right((files))
  }

  // get all the paths in the zip with various bits of validation
  def getZippedFiles(zip: ZipFile): Either[ZipError, List[ZipEntry]] = {
    val entries = zip.entries().asScala.toList
    validateZip(entries)
  }

  def readFileFromZip(zip: ZipFile, entry: ZipEntry): Array[Byte] = {
    // Bound actual decompression too: ZIP size metadata cannot be trusted.
    val bytes = Using.resource(zip.getInputStream(entry))(_.readNBytes(MAX_FILE_SIZE + 1))
    if (bytes.length > MAX_FILE_SIZE || bytes.length.toLong != entry.getSize) throw new ZipException("ZIP has invalid file sizes.")
    bytes
  }
}
