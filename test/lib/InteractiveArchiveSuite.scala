package lib

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.Files
import java.util.zip.{ZipEntry, ZipException, ZipFile, ZipOutputStream}
import scala.util.Using

class InteractiveArchiveSuite extends munit.FunSuite {
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
      val files = InteractiveArchive.files(zip)
      assertEquals(files.map(_.getName), Vector("index.html", "assets/main.js"))
      assertEquals(InteractiveArchive.read(zip, files.head).toSeq, html.toSeq)
    }
  }

  test("requires a root index.html") {
    withZip(Seq("nested/index.html" -> html)) { zip =>
      intercept[ZipException](InteractiveArchive.files(zip))
    }
  }

  test("rejects paths that escape the project's URL prefix") {
    Seq("../escape", "/absolute", "nested/../../escape", "C:/escape", "nested\\escape", "a//b").foreach { unsafe =>
      withZip(Seq("index.html" -> html, unsafe -> html)) { zip =>
        intercept[ZipException](InteractiveArchive.files(zip))
      }
    }
  }

  test("limits per-file expansion before uploading") {
    withZip(Seq("index.html" -> new Array[Byte](InteractiveArchive.maxFileBytes + 1))) { zip =>
      intercept[ZipException](InteractiveArchive.files(zip))
    }
  }

  test("limits total expansion before uploading") {
    val bytes = new Array[Byte](45 * 1024 * 1024)
    withZip(Seq("index.html" -> html) ++ (1 to 5).map(i => s"$i.bin" -> bytes)) { zip =>
      intercept[ZipException](InteractiveArchive.files(zip))
    }
  }

  test("limits entry count") {
    withZip((1 to InteractiveArchive.maxFiles + 1).map(i => s"$i.txt" -> Array.emptyByteArray)) { zip =>
      intercept[ZipException](InteractiveArchive.files(zip))
    }
  }

  test("checks actual file size against ZIP metadata") {
    withZip(Seq("index.html" -> html)) { zip =>
      val entry = new ZipEntry(zip.getEntry("index.html"))
      entry.setSize(0)
      intercept[ZipException](InteractiveArchive.read(zip, entry))
    }
  }

  test("normalises project names and supplies a fallback") {
    assertEquals(InteractiveArchive.projectSlug("My Interactive.zip"), "my-interactive")
    assertEquals(InteractiveArchive.projectSlug("../"), "visual")
    assertEquals(InteractiveArchive.projectSlug("x" * 100).length, 80)
  }
}
