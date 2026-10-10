#!/usr/bin/env bash
set -euo pipefail
mvn clean package
docker compose up -d --build
docker compose logs -f
