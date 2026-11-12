package lib

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipException, ZipFile, ZipOutputStream}
import scala.util.Using
import lib.InteractiveZip._

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
      val files = InteractiveZip.getZippedFiles(zip).fold(error => fail(error.msg), identity)
      assertEquals(files.map(_.getName), List("index.html", "assets/main.js"))
      assertEquals(InteractiveZip.readFileFromZip(zip, files.head).toSeq, html.toSeq)
    }
  }

  test("requires a root index.html") {
    withZip(Seq("nested/index.html" -> html)) { zip =>
      assertEquals(InteractiveZip.getZippedFiles(zip), Left(MissingIndexHtmlError))
    }
  }

  test("rejects paths that escape the project's URL prefix") {
    Seq("../escape", "/absolute", "nested/../../escape", "C:/escape", "nested\\escape", "a//b").foreach { unsafe =>
      withZip(Seq("index.html" -> html, unsafe -> html)) { zip =>
        assertEquals(InteractiveZip.getZippedFiles(zip), Left(UnsafeFilePathError))
      }
    }
  }

  test("limits per-file expansion before uploading") {
    withZip(Seq("index.html" -> new Array[Byte](InteractiveZip.MAX_FILE_SIZE + 1))) { zip =>
      assertEquals(InteractiveZip.getZippedFiles(zip), Left(OverallZipTooLargeError))
    }
  }

  test("limits total expansion before uploading") {
    val bytes = new Array[Byte](45 * 1024 * 1024)
    withZip(Seq("index.html" -> html) ++ (1 to 5).map(i => s"$i.bin" -> bytes)) { zip =>
      assertEquals(InteractiveZip.getZippedFiles(zip), Left(OverallZipTooLargeError))
    }
  }

  test("limits number of files in zip") {
    withZip(Seq("index.html" -> html) ++ (1 to InteractiveZip.MAX_FILES_IN_ZIP).map(i => s"$i.txt" -> Array.emptyByteArray)) { zip =>
      assertEquals(InteractiveZip.getZippedFiles(zip), Left(TooManyFilesError))
    }
  }

  test("checks actual file size against ZIP metadata") {
    withZip(Seq("index.html" -> html)) { zip =>
      val entry = new ZipEntry(zip.getEntry("index.html"))
      entry.setSize(0)
      intercept[ZipException](InteractiveZip.readFileFromZip(zip, entry))
    }
  }

  test("rejects duplicate file paths") {
    val entries = List(new ZipEntry("index.html"), new ZipEntry("index.html"))
    assertEquals(InteractiveZip.validateZip(entries), Left(DuplicateFilePathsError))
  }

  test("normalises project names and supplies a dated fallback") {
    assertEquals(InteractiveZip.projectSlug("My Interactive.zip"), "my-interactive")
    assert(InteractiveZip.projectSlug("../").matches("interactive-upload-\\d{4}-\\d{2}-\\d{2}"))
    assertEquals(InteractiveZip.projectSlug("x" * 100).length, 80)
  }
}
