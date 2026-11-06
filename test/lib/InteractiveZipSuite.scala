package lib

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipException, ZipFile, ZipOutputStream}
import scala.util.Using

class InteractiveZipSuite extends munit.FunSuite {
  private val html = "<html></html>".getBytes(UTF_8)

  private def withZip(entries: Seq[(String, Array[Byte])])(run: ZipFile => Unit): Unit = {
    val path = Files.createTempFile("interactive-test-", ".zip")
    try {
      Using.resource(new ZipOutputStream(Files.newOutputStream(path))) { zip =>
        entries.foreach { case (name, bytes) =>
          zip.putNextEntry(new ZipEntry(name))
          zip.write(bytes)
          zip.closeEntry()
        }
      }
      Using.resource(new ZipFile(path.toFile))(run)
    } finally Files.deleteIfExists(path)
  }

  test("preserves nested files, reads their contents and excludes Mac metadata") {
    withZip(Seq("index.html" -> html, "assets/main.js" -> html, "__MACOSX/._index.html" -> html, ".DS_Store" -> html)) { zip =>
      val files = InteractiveZip.getZippedFiles(zip)
      assertEquals(files.map(_.getName), Vector("index.html", "assets/main.js"))
      assertEquals(InteractiveZip.readFileFromZip(zip, files.head).toSeq, html.toSeq)
    }
  }

  test("requires a root index.html") {
    withZip(Seq("nested/index.html" -> html)) { zip =>
      intercept[ZipException](InteractiveZip.getZippedFiles(zip))
    }
  }

  test("rejects paths that escape the project's URL prefix") {
    Seq("../escape", "/absolute", "nested/../../escape", "C:/escape", "nested\\escape", "a//b").foreach { unsafe =>
      withZip(Seq("index.html" -> html, unsafe -> html)) { zip =>
        intercept[ZipException](InteractiveZip.getZippedFiles(zip))
      }
    }
  }

  test("limits per-file expansion before uploading") {
    withZip(Seq("index.html" -> new Array[Byte](InteractiveZip.MAX_FILE_SIZE + 1))) { zip =>
      intercept[ZipException](InteractiveZip.getZippedFiles(zip))
    }
  }

  test("limits total expansion before uploading") {
    val bytes = new Array[Byte](45 * 1024 * 1024)
    withZip(Seq("index.html" -> html) ++ (1 to 5).map(i => s"$i.bin" -> bytes)) { zip =>
      intercept[ZipException](InteractiveZip.getZippedFiles(zip))
    }
  }

  test("limits number of files in zip") {
    withZip((1 to InteractiveZip.MAX_FILES_IN_ZIP + 1).map(i => s"$i.txt" -> Array.emptyByteArray)) { zip =>
      intercept[ZipException](InteractiveZip.getZippedFiles(zip))
    }
  }

  test("checks actual file size against ZIP metadata") {
    withZip(Seq("index.html" -> html)) { zip =>
      val entry = new ZipEntry(zip.getEntry("index.html"))
      entry.setSize(0)
      intercept[ZipException](InteractiveZip.readFileFromZip(zip, entry))
    }
  }

  test("normalises project names and supplies a fallback") {
    assertEquals(InteractiveZip.projectSlug("My Interactive.zip"), "my-interactive")
    assertEquals(InteractiveZip.projectSlug("../"), "visual")
    assertEquals(InteractiveZip.projectSlug("x" * 100).length, 80)
  }
}
