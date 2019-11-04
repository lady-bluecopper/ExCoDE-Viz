#!/usr/bin/env bash

SOURCED=false && [ "$0" = "$BASH_SOURCE" ] || SOURCED=true

if ! $SOURCED; then
  set -euo pipefail
  IFS=$'\n\t'
fi

SCRIPTDIR="$(cd -P -- "$(dirname -- "$0")" && pwd -P)"

# remore and recreates the uploads folder
rm -rf "${SCRIPTDIR}"/app/uploads/*.csv
mkdir -p "${SCRIPTDIR}"/app/uploads

# rm -rf "${SCRIPTDIR}"/uploads
# mkdir -p "${SCRIPTDIR}"/uploads

# build docker image
docker build -t excode-demo -f flask-server.dockerfile .

docker run \
         --rm \
         -it \
         --publish=80:80 \
         -v"${SCRIPTDIR}"/app/static:/app/static \
         -v"${SCRIPTDIR}"/app/templates:/app/templates \
         -v"${SCRIPTDIR}"/logs:/logs \
         -v"${SCRIPTDIR}"/app/uploads:/app/uploads \
         -v"${SCRIPTDIR}"/uploads:/uploads \
         excode-demo

exit 0
