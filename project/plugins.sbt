logLevel := Level.Warn

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.11")

addSbtPlugin("com.typesafe.sbt" % "sbt-native-packager" % "1.0.6")

addSbtPlugin("com.gu" % "sbt-riffraff-artifact" % "0.9.7")

addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")