package lib

import java.nio.charset.StandardCharsets.UTF_8
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}

class InteractiveArchiveSuite extends munit.FunSuite {
  private def withZip(entries: Seq[(String, Array[Byte])])(run: Path => Unit): Unit = {
    val path = Files.createTempFile("interactive-test-", ".zip")
    val zip = new ZipOutputStream(Files.newOutputStream(path))
    try entries.foreach { case (name, bytes) =>
      zip.putNextEntry(new ZipEntry(name))
      zip.write(bytes)
      zip.closeEntry()
    } finally zip.close()
    try run(path) finally Files.deleteIfExists(path)
  }

  private def bytes(value: String): Array[Byte] = value.getBytes(UTF_8)

  test("preserves nested assets, filters Mac files, and assigns web content types") {
    withZip(Seq("index.html" -> bytes("<html></html>"), "assets/main.js" -> bytes("alert(1)"),
      "assets/style.css" -> bytes("body {}"), "__MACOSX/._index.html" -> bytes("metadata"),
      ".DS_Store" -> bytes("metadata"))) { path =>
      var extracted: Option[Path] = None
      InteractiveArchive.withAssets(path) { assets =>
        assertEquals(assets.map(_.relativePath).toSet, Set("index.html", "assets/main.js", "assets/style.css"))
        assertEquals(assets.find(_.relativePath.endsWith(".js")).map(_.contentType), Some("text/javascript; charset=utf-8"))
        assertEquals(Files.readString(assets.head.path), "<html></html>")
        extracted = Some(assets.head.path.getParent)
      }
      assert(!Files.exists(extracted.get))
    }
  }

  test("requires index.html at the root before publishing") {
    withZip(Seq("folder/index.html" -> bytes("html"))) { path =>
      intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
    }
  }

  test("rejects traversal, absolute paths and Windows paths") {
    Seq("../escape", "/absolute", "nested/../../escape", "C:/escape", "nested\\escape").foreach { unsafe =>
      withZip(Seq("index.html" -> bytes("html"), unsafe -> bytes("bad"))) { path =>
        intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
      }
    }
  }

  test("limits expanded file sizes even when compressed ZIP is small") {
    withZip(Seq("index.html" -> new Array[Byte]((InteractiveArchive.maxFileBytes + 1).toInt))) { path =>
      intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
    }
  }

  test("rejects conflicting file and directory paths") {
    withZip(Seq("index.html" -> bytes("html"), "assets" -> bytes("file"), "assets/script.js" -> bytes("js"))) { path =>
      intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
    }
  }

  test("rejects invalid ZIPs") {
    val path = Files.createTempFile("invalid-interactive-", ".zip")
    try {
      Files.writeString(path, "not a ZIP")
      intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
    } finally Files.deleteIfExists(path)
  }

  test("limits ignored metadata expansion too") {
    withZip(Seq("index.html" -> bytes("html"), "__MACOSX/large" -> new Array[Byte]((InteractiveArchive.maxFileBytes + 1).toInt))) { path =>
      intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
    }
  }

  test("limits entry count") {
    withZip((1 to InteractiveArchive.maxFiles + 1).map(i => s"$i.txt" -> Array.emptyByteArray)) { path =>
      intercept[InvalidInteractive] { InteractiveArchive.withAssets(path)(_ => fail("must not publish")) }
    }
  }

  test("cleans extracted files when publishing fails") {
    withZip(Seq("index.html" -> bytes("html"))) { path =>
      var extracted: Option[Path] = None
      intercept[IllegalStateException] {
        InteractiveArchive.withAssets(path) { assets =>
          extracted = Some(assets.head.path.getParent)
          throw new IllegalStateException("S3 unavailable")
        }
      }
      assert(!Files.exists(extracted.get))
    }
  }

  test("normalises project names and supplies a fallback") {
    assertEquals(InteractiveArchive.projectSlug("My Interactive.zip"), "my-interactive")
    assertEquals(InteractiveArchive.projectSlug("../"), "visual")
    assertEquals(InteractiveArchive.projectSlug("x" * 100).length, 80)
  }
}
