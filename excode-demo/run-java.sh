#!/usr/bin/env bash

SOURCED=false && [ "$0" = "$BASH_SOURCE" ] || SOURCED=true

if ! $SOURCED; then
  set -euo pipefail
  IFS=$'\n\t'
fi

SCRIPTDIR="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"

# build jar
(cd "${SCRIPTDIR}"/javapp/ && mvn clean package) 

# build docker image
docker build -t excode-demo-java -f java-app.dockerfile .

docker run \
         --rm \
         -it \
         --publish=8082:8082 \
         -v"${SCRIPTDIR}"/uploads:/uploads \
         -v"${SCRIPTDIR}"/app/uploads:/app/uploads \
          excode-demo-java

exit 0