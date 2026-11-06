package lib

import java.util.Locale
import java.util.zip.{ZipEntry, ZipException, ZipFile}
import scala.jdk.CollectionConverters._
import scala.util.Using

object InteractiveArchive {
  val maxFiles = 2000
  val maxFileBytes = 50 * 1024 * 1024
  val maxExpandedBytes = 200L * 1024 * 1024

  def projectSlug(name: String): String = {
    val slug = name.toLowerCase(Locale.ROOT).trim.stripSuffix(".zip")
      .replaceAll("[^a-z0-9_-]+", "-").stripPrefix("-").stripSuffix("-").take(80)
    if (slug.isEmpty) "visual" else slug
  }

  def files(zip: ZipFile): Vector[ZipEntry] = {
    val entries = zip.entries().asScala.take(maxFiles + 1).toVector
    if (entries.size > maxFiles) throw new ZipException(s"ZIP must contain at most $maxFiles entries.")
    val files = entries.filterNot(entry => entry.isDirectory || entry.getName.split("/").exists { part =>
      part == "__MACOSX" || part == ".DS_Store" || part.startsWith("._")
    })
    val names = files.map(_.getName)
    if (!names.contains("index.html")) throw new ZipException("ZIP must contain index.html at its root.")
    if (names.distinct.size != names.size) throw new ZipException("ZIP contains duplicate file paths.")
    names.foreach { name =>
      if (name.startsWith("/") || name.contains("\\") || name.contains(":") || name.exists(Character.isISOControl) ||
          name.split("/", -1).exists(part => part.isEmpty || part == "." || part == "..")) {
        throw new ZipException("ZIP contains an unsafe file path.")
      }
    }
    if (files.exists(entry => entry.getSize < 0 || entry.getSize > maxFileBytes) || files.map(_.getSize).sum > maxExpandedBytes) {
      throw new ZipException("Expanded ZIP exceeds the 50 MB per file or 200 MB total limit.")
    }
    files
  }

  def read(zip: ZipFile, entry: ZipEntry): Array[Byte] = {
    // Bound actual decompression too: ZIP size metadata cannot be trusted.
    val bytes = Using.resource(zip.getInputStream(entry))(_.readNBytes(maxFileBytes + 1))
    if (bytes.length > maxFileBytes || bytes.length.toLong != entry.getSize) throw new ZipException("ZIP has invalid file sizes.")
    bytes
  }
}
