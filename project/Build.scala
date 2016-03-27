import com.typesafe.sbt.web.Import
import com.typesafe.sbt.web.SbtWeb
import play.sbt.PlayScala
import sbt._
import play.sbt.Play.autoImport._
import sbt.Keys._
import org.ensime.sbt.Plugin.Settings.ensimeConfig
import org.ensime.sbt.util.SExp._

object ApplicationBuild extends Build {
  val appName = "Web_Clipboard"

  val appVersion = "0.1"

  val appDependencies = Seq(
    jdbc,
    "postgresql" % "postgresql" % "9.1-901.jdbc4",
    // "org.fusesource.scalate" %% "scalate-core" % "1.6.1",
    "org.scalatra.scalate" %% "scalate-core" % "1.7.1",
    "org.squeryl" %% "squeryl" % "0.9.5-6",
    "org.scalaz" %% "scalaz-core" % "6.0.4",
    "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.2",

    "org.webjars" %% "webjars-play" % "2.4.0",
    "org.webjars" % "bootstrap" % "3.3.6",
    "org.webjars" % "jquery" % "2.2.2"
  )

  val appResolvers = Seq()

  val appSettings = Seq(
    // resolvers ++= appResolvers,
    scalacOptions ++= Seq("-Xlint", "-deprecation"),
    javaOptions in run += "-Xmx2G -XX:MaxPermSize=512m"
    /*ensimeConfig := sexp(
      key(":formatting-prefs"), sexp(
        key(":preserveDanglingCloseParenthesis"), true,
        key(":alignParameters"), true,
        key(":doubleIndentClassDeclaration"), false
      )
    ),*/
  )
  // lazy val root = (project in file(".")).enablePlugins(PlayScala)

  val main = Project(appName, file(".")).enablePlugins(play.PlayScala, SbtWeb).settings(
    version := appVersion,
    scalaVersion := "2.10.5",
    libraryDependencies ++= appDependencies,
    watchSources <++= baseDirectory map { path => ((path / "app" / "views") ** "*.jade").get }
  )

  // Import.pipelineStages := Seq(RjsKeys.rjs)
}
