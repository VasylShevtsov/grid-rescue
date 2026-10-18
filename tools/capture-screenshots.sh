#!/bin/sh
# Render the real Swing view after exercising its controls, without a desktop.
set -eu
cd "$(dirname "$0")/.."
mvn --batch-mode test-compile org.apache.maven.plugins:maven-dependency-plugin:3.8.1:build-classpath \
  -DincludeScope=test -Dmdep.outputFile=target/screenshot-classpath.txt
if [ -n "${JAVA_HOME:-}" ]; then
  screenshot_java="$JAVA_HOME/bin/java"
else
  screenshot_java=java
fi
"$screenshot_java" -Djava.awt.headless=true \
  -cp "target/classes:target/test-classes:$(cat target/screenshot-classpath.txt)" \
  edu.whitman.ui.MissionScreenshots "${1:-docs/screenshots}"
