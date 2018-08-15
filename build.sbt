val V = new {
  val awsSdk = "1.11.313"
  val circe = "0.9.3"
  val dockerTestKit = "0.9.5"
  val postgresql = "42.2.2"
  val scalatest = "3.0.5"
}

val commonsSettings = Seq(
  organization := "com.hbc",
  scalaVersion := "2.12.6",
  scalacOptions ++= Seq(
    "-encoding",
    "UTF-8",
    "-deprecation",
    "-feature",
    "-unchecked",
    "-language:higherKinds",
    "-language:implicitConversions"
  ),
  libraryDependencies ++= Seq(
    "com.amazonaws"     % "aws-java-sdk-cloudwatch"     % V.awsSdk,
    "com.amazonaws"     % "aws-java-sdk-dynamodb"       % V.awsSdk,
    "com.amazonaws"     % "aws-java-sdk-kinesis"        % V.awsSdk,
    "org.scalatest"    %% "scalatest"                   % V.scalatest,
    "com.whisk"        %% "docker-testkit-scalatest"    % V.dockerTestKit,
    "com.whisk"        %% "docker-testkit-impl-spotify" % V.dockerTestKit,
    "cloud.localstack"  % "localstack-utils"            % "0.1.13",
    "com.twitter"      %% "finagle-redis"               % "18.4.0",
    "io.circe"         %% "circe-core"                  % V.circe,
    "io.circe"         %% "circe-generic"               % V.circe,
    "io.circe"         %% "circe-parser"                % V.circe,
    "org.postgresql"    % "postgresql"                  % "42.2.2"
  )
)

val publishSettings = Seq(
  publishMavenStyle := true,
  Test / publishArtifact := false,
  publishTo := Some("Artifactory Realm" at "https://hbc.jfrog.io/hbc/hbcbay-sbt"),
  pomIncludeRepository := (_ => false),
  pomExtra :=
    <scm>
      <url>https://github.com/saksdirect/hbc-docker-it.git</url>
      <connection>scm:git:git@github.com:saksdirect/hbc-docker-it.git</connection>
    </scm>
      <developers>
        <developer>
          <id>wham</id>
          <name>HBC Wham! Team</name>
          <url>https://github.com/saksdirect/wham</url>
        </developer>
      </developers>
)

val testSettings = Seq(
  libraryDependencies ++= Seq(
    "org.scalatest" %% "scalatest"   % V.scalatest % Test
  )
)

parallelExecution in Test := false

lazy val `hbc-docker-it` = project
  .in(file("."))
  .settings(
    name := "hbc-docker-it"
  )
  .settings(commonsSettings ++ publishSettings ++ testSettings: _*)
  .enablePlugins(GitVersioning)
  .settings(
    git.useGitDescribe := true
  )
