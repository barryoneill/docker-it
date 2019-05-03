val `scala 212` = "2.12.8"

val scalacOpts = Seq(
  "-deprecation", // Emit warning and location for usages of deprecated APIs.
  "-encoding",
  "utf-8", // Specify character encoding used by source files.
  "-explaintypes", // Explain type errors in more detail.
  "-feature", // Emit warning and location for usages of features that should be imported explicitly.
  "-language:existentials", // Existential types (besides wildcard types) can be written and inferred
  "-language:experimental.macros", // Allow macro definition (besides implementation and application)
  "-language:higherKinds", // Allow higher-kinded types
  "-language:implicitConversions", // Allow definition of implicit functions called views
  "-unchecked", // Enable additional warnings where generated code depends on assumptions.
  "-Xcheckinit", // Wrap field accessors to throw an exception on uninitialized access.
  "-Xfatal-warnings", // Fail the compilation if there are any warnings.
  "-Xfuture", // Turn on future language features.
  "-Xlint:adapted-args", // Warn if an argument list is modified to match the receiver.
  "-Xlint:by-name-right-associative", // By-name parameter of right associative operator.
  "-Xlint:delayedinit-select", // Selecting member of DelayedInit.
  "-Xlint:doc-detached", // A Scaladoc comment appears to be detached from its element.
  "-Xlint:inaccessible", // Warn about inaccessible types in method signatures.
  "-Xlint:infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Xlint:missing-interpolator", // A string literal appears to be missing an interpolator id.
  "-Xlint:nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Xlint:nullary-unit", // Warn when nullary methods return Unit.
  "-Xlint:option-implicit", // Option.apply used implicit view.
  "-Xlint:package-object-classes", // Class or object defined in package object.
  "-Xlint:poly-implicit-overload", // Parameterized overloaded implicit methods are not visible as view bounds.
  "-Xlint:private-shadow", // A private field (or class parameter) shadows a superclass field.
  "-Xlint:stars-align", // Pattern sequence wildcard must align with sequence component.
  "-Xlint:type-parameter-shadow", // A local type parameter shadows a type already in scope.
  "-Xlint:unsound-match", // Pattern match may not be typesafe.
  "-Yno-adapted-args", // Do not adapt an argument list (either by inserting () or creating a tuple) to match the receiver.
  "-Ypartial-unification", // Enable partial unification in type constructor inference
  "-Ywarn-dead-code", // Warn when dead code is identified.
  "-Ywarn-inaccessible", // Warn about inaccessible types in method signatures.
  "-Ywarn-infer-any", // Warn when a type argument is inferred to be `Any`.
  "-Ywarn-nullary-override", // Warn when non-nullary `def f()' overrides nullary `def f'.
  "-Ywarn-nullary-unit", // Warn when nullary methods return Unit.
  "-Ywarn-numeric-widen" // Warn when numerics are widened.
)

val V = new {
  val awsSdk          = "1.11.545"
  val circe           = "0.11.1"
  val dockerTestKit   = "0.9.8"
  val postgresql      = "42.2.5"
  val scalatest       = "3.0.7"
  val localstackUtils = "0.1.19"
  val finagleRedis    = "19.4.0"
  val postgreSQL      = "42.2.5"
  val kafka           = "2.2.0"
  val log4j           = "1.7.26"
}

lazy val appDependencies = Seq(
  "com.amazonaws"    % "aws-java-sdk-cloudwatch"      % V.awsSdk,
  "com.amazonaws"    % "aws-java-sdk-dynamodb"        % V.awsSdk,
  "com.amazonaws"    % "aws-java-sdk-kinesis"         % V.awsSdk,
  "org.scalatest"    %% "scalatest"                   % V.scalatest,
  "com.whisk"        %% "docker-testkit-scalatest"    % V.dockerTestKit,
  "com.whisk"        %% "docker-testkit-impl-spotify" % V.dockerTestKit,
  "cloud.localstack" % "localstack-utils"             % V.localstackUtils,
  "com.twitter"      %% "finagle-redis"               % V.finagleRedis,
  "io.circe"         %% "circe-core"                  % V.circe,
  "io.circe"         %% "circe-generic"               % V.circe,
  "io.circe"         %% "circe-parser"                % V.circe,
  "org.postgresql"   % "postgresql"                   % V.postgreSQL,
  "org.apache.kafka" %% "kafka"                       % V.kafka,
  "org.slf4j"        % "slf4j-log4j12"                % V.log4j,
  "org.scalatest"    %% "scalatest"                   % V.scalatest % Test
)

parallelExecution in Test := false

lazy val root = project
  .in(file("."))
  .settings(
    name                := "docker-it",
    organization        := "com.hbc",
    scalaVersion        := `scala 212`,
    scalacOptions       ++= scalacOpts,
    libraryDependencies ++= appDependencies,
    git.useGitDescribe  := true,
    addCommandAlias("format", ";scalafmt;test:scalafmt;scalafmtSbt"),
    addCommandAlias(
      "updates",
      ";dependencyUpdates; reload plugins; dependencyUpdates;reload return"
    ),
    addCommandAlias("fullBuild", ";checkFormat;clean;test"),
    addCommandAlias(
      "fullCiBuild",
      ";set scalacOptions in ThisBuild ++= Seq(\"-opt:l:inline\", \"-opt-inline-from:**\");fullBuild"
    ),
    addCommandAlias(
      "checkFormat",
      ";scalafmtCheck;test:scalafmtCheck;scalafmtSbtCheck"
    )
  )
  .enablePlugins(GitVersioning)
