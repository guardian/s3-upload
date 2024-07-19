logLevel := Level.Warn

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))

addSbtPlugin("org.playframework" % "sbt-plugin" % "3.0.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-digest" % "1.1.4")

addSbtPlugin("com.typesafe.sbt" % "sbt-gzip" % "1.0.2")
