#!/usr/bin/env bash

set -eu
apk add --no-cache bash git

# 1st param... The git URL to clone
# 2nd param... The tag name to check out
function download_target_project() {
  DIR_NAME=$(mktemp -d)
  cd /$DIR_NAME
  git clone "$1" target_repo
  cd target_repo
  git checkout "$2"
}

function run_smoke_test() {
  echo -n waiting SonarQube
  until $(curl --output /dev/null -s --fail http://sonarqube:9000); do
    echo -n '.'
    sleep 5
  done
  echo SonarQube has been launched.

  count=0
  until mvn compile org.eclipse.jetty:jetty-jspc-maven-plugin:jspc org.sonarsource.scanner.maven:sonar-maven-plugin:3.5.0.1254:sonar -B -Dmaven.test.skip -Dsonar.profile="FindBugs + FB-Contrib" -Dsonar.host.url=http://sonarqube:9000 -Dsonar.login=admin -Dsonar.password=admin; do
    count=$[ $count + 1 ]
    if [ $count -ge 5 ]; then
      echo Sonar fails to scan 5 times!
      exit 1
    fi
    echo SonarQube is not ready to scan project, wait 5 sec
    sleep 5
  done
}

# Use the project that uses Maven and contains .jsp file
download_target_project 'https://github.com/spring-projects/spring-petclinic.git' 'e9f5f7b54108e35e660a9c9311a682ddce0633bc'
run_smoke_test
