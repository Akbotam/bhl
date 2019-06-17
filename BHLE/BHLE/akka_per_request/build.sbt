name := "akka_per_request"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= {
    val akkaStreamVersion = "2.0-M2"
    val akkaVersion = "2.3.12"
    val scalaTestVersion       = "2.2.4"
    val scalaMockVersion       = "3.2.2"
    val slickVersion = "3.2.3"
    val json4sVersion = "3.5.4"
    lazy val alpakkaVersion = "0.18"
    Seq(
        "com.typesafe.akka"   %% "akka-actor"                           % akkaVersion,
        "com.typesafe.akka"   %% "akka-stream-experimental"             % akkaStreamVersion,
        "com.typesafe.akka"   %% "akka-http-experimental"               % akkaStreamVersion,
        "com.typesafe.akka"   %% "akka-http-core-experimental"          % akkaStreamVersion,
        "com.typesafe.akka"   %% "akka-http-spray-json-experimental"    % akkaStreamVersion,
        "com.typesafe.akka"   %% "akka-http-testkit-experimental"       % akkaStreamVersion,
        "com.typesafe.akka"   %% "akka-slf4j"                           % akkaVersion % "test",
        "com.typesafe.akka"   %% "akka-testkit"                         % akkaVersion % "test",

        "com.typesafe.slick"  %% "slick"                                % slickVersion,
        "com.typesafe.slick"  %% "slick-hikaricp"                       % slickVersion,

        "org.slf4j"           %  "slf4j-nop"                            % "1.6.4",

        "org.postgresql"      %  "postgresql"                           % "9.4-1201-jdbc4",
        "org.flywaydb"        %  "flyway-core"                          % "3.2.1",

        "org.scalatest"       %% "scalatest"                            % scalaTestVersion,
        "org.scalamock"       %% "scalamock-scalatest-support"          % scalaMockVersion,

        "com.rabbitmq"        % "amqp-client"                           % "3.5.2",
        "ch.qos.logback"      %  "logback-classic"                      % "1.1.2" % "test",
        "junit"           	  % "junit"                                 % "4.12" % "test",
        "com.github.sstone"   % "amqp-client_2.11"                      % "1.5",

        "com.typesafe.akka"   %% "akka-http-spray-json"                 % "10.0.11",
        "net.liftweb"         %% "lift-json"                            % "2.6",
        "com.lightbend.akka"  %% "akka-stream-alpakka-slick"            % "1.0-M2",
        "org.json4s"          %% "json4s-core"                          % json4sVersion,
        "org.json4s"          %% "json4s-jackson"                       % json4sVersion,
        "org.json4s"          %% "json4s-native"                        % json4sVersion,
        "de.heikoseeberger"   %% "akka-http-json4s"                     % "1.20.1",
        "com.lightbend.akka"  %% "akka-stream-alpakka-amqp"             % alpakkaVersion

    )
}