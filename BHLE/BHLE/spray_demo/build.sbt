name := "spray_demo"

organization := "one.tech"


version := "0.1"

scalaVersion := "2.11.7"

val sprayVersion = "1.3.4"
val akkaVersion = "2.5.19"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor" % akkaVersion,
    "io.spray" %% "spray-can" % sprayVersion,
    "io.spray" %% "spray-routing" % sprayVersion,
    "org.json4s" %% "json4s-native" % "3.6.4"
)