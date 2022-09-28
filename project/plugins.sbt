addSbtPlugin("com.jsuereth" % "sbt-pgp" % "1.1.0")
addSbtPlugin("org.xerial.sbt" % "sbt-sonatype" % "2.0")
addSbtPlugin("com.github.gseitz" % "sbt-release" % "1.0.6")

resolvers += Resolver.typesafeRepo("releases")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.8.0")
addSbtPlugin("net.virtual-void" % "sbt-dependency-graph" % "0.9.2")