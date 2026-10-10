#!/usr/bin/env sh
set -eu
mvn verify
docker compose up --build -d --wait
