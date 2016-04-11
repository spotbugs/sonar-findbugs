#!/bin/bash

set -euo pipefail

case "$TEST" in

ci)
  mvn verify -B -e -V
  ;;
*)
  echo "Unexpected TEST mode: $TEST"
  exit 1
  ;;

esac
