package lib

import java.lang.reflect.{InvocationHandler, Method, Proxy}
import java.nio.file.{Files, Path}
import java.util.zip.{ZipEntry, ZipOutputStream}
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model._
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class InteractivePublisherSuite extends munit.FunSuite {
  private class FakeS3(failOnIndex: Boolean = false) {
    val puts = mutable.ArrayBuffer.empty[PutObjectRequest]
    val deletes = mutable.ArrayBuffer.empty[String]
    val client: S3Client = Proxy.newProxyInstance(classOf[S3Client].getClassLoader, Array(classOf[S3Client]), new InvocationHandler {
      override def invoke(proxy: Any, method: Method, args: Array[AnyRef]): AnyRef = method.getName match {
        case "putObject" =>
          val request = args(0).asInstanceOf[PutObjectRequest]
          if (failOnIndex && request.key().endsWith("/index.html")) throw new IllegalStateException("S3 unavailable")
          puts += request
          PutObjectResponse.builder().build()
        case "deleteObject" =>
          deletes += args(0).asInstanceOf[DeleteObjectRequest].key()
          DeleteObjectResponse.builder().build()
        case other => throw new UnsupportedOperationException(other)
      }
    }).asInstanceOf[S3Client]
  }

  private def config(client: S3Client): Config = new Config {
    val bucketName = "test-interactives"
    val region = "eu-west-1"
    val s3Client = client
    val maybePrettyBaseUrl = Some("https://interactive.example.com")
    def key(fileName: String): String = s"uploader/code/embed/$fileName"
  }

  private def withZip(run: Path => Unit): Unit = {
    val archive = Files.createTempFile("interactive-publish-test-", ".zip")
    val zip = new ZipOutputStream(Files.newOutputStream(archive))
    try Seq("index.html", "assets/my script.js").foreach { name =>
      zip.putNextEntry(new ZipEntry(name))
      zip.write("test".getBytes(java.nio.charset.StandardCharsets.UTF_8))
      zip.closeEntry()
    } finally zip.close()
    try run(archive) finally Files.deleteIfExists(archive)
  }

  test("publishes a unique bundle, preserves paths, and returns encoded public URLs") {
    withZip { archive =>
      val fake = new FakeS3()
      val publisher = new InteractivePublisher(config(fake.client))
      val result = publisher.publish(archive, "My Project", "editor@example.com")
      assertEquals(fake.puts.map(_.bucket()).toSet, Set("test-interactives"))
      assert(fake.puts.head.key().endsWith("/assets/my script.js"))
      assert(fake.puts.last.key().endsWith("/index.html"))
      assert(fake.puts.last.key().contains("/my-project/"))
      assertEquals(fake.puts.head.contentType(), "text/javascript; charset=utf-8")
      assertEquals(fake.puts.head.metadata().asScala.toMap, Map("author" -> "editor@example.com"))
      assertEquals(fake.puts.head.cacheControl(), "max-age=604800")
      assertEquals(fake.puts.head.acl(), ObjectCannedACL.PUBLIC_READ)
      assert(result.url.startsWith("https://interactive.example.com/uploader/code/embed/"))
      assert(result.url.endsWith("/index.html"))
      assert(result.files.exists(_._2.endsWith("/assets/my%20script.js")))
      val another = publisher.publish(archive, "My Project", "editor@example.com")
      assertNotEquals(result.url, another.url)
    }
  }

  test("removes uploaded assets after a later upload fails") {
    withZip { archive =>
      val fake = new FakeS3(failOnIndex = true)
      intercept[IllegalStateException] {
        new InteractivePublisher(config(fake.client)).publish(archive, "project", "editor@example.com")
      }
      assertEquals(fake.deletes.toSeq, fake.puts.map(_.key()).toSeq)
      assertEquals(fake.deletes.size, 1)
    }
  }
}
