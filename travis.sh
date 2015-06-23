#!/bin/bash

set -euo pipefail

function installTravisTools {
  curl -sSL https://raw.githubusercontent.com/sonarsource/travis-utils/master/install.sh | bash
}

if [ "$TESTS" == "CI" ]; then
  mvn verify -B -e -V
else
  mvn install -DskipTests=true

  installTravisTools

  # travis_run_its "SONAR_VERSION" ["DEPENDENCY1"] ["DEPENDENCY2"]
  travis_run_its "${TESTS}" "SonarSource/sonar-java"
fi
