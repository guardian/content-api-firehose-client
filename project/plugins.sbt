resolvers += Resolver.typesafeRepo("releases")
addSbtPlugin("org.scalariform" % "sbt-scalariform" % "1.6.0")

resolvers += "twitter-repo" at "https://maven.twttr.com"
addSbtPlugin("com.twitter" %% "scrooge-sbt-plugin" % "4.6.0")