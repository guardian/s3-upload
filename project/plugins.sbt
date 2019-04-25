logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

libraryDependencies += "org.vafer" % "jdeb" % "1.3" artifacts (Artifact("jdeb", "jar", "jar"))

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.7.1")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "1.1.9")

addSbtPlugin("io.get-coursier" % "sbt-coursier" % "1.1.0-M13")