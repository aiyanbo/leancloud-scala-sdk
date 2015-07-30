name := "leancloud-scala-sdk"

version := "1.0.0-SNAPSHOT"

organization := "org.jmotor.leancloud"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe" % "config" % "1.3.0",
  "com.ning" % "async-http-client" % "1.9.22",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)
