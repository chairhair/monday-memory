#!/usr/bin/env bash

set -e

DEBUG=false

while [[ "$#" -gt 0 ]]; do
  case "$1" in
    -d|--debug)
      DEBUG=true
      shift
      ;;
    *)
      echo "❌ Unknown option: $1"
      echo "Usage: ./run.sh [-d|--debug]"
      exit 1
      ;;
  esac
done

source .env

cd docker
docker compose down -v
docker compose up -d
cd ../

if [ "$DEBUG" = true ]; then
  echo "Starting in DEBUG mode (JDWP on port 5005)..."
  gradle clean bootRun --debug-jvm
else
  echo "🚀 Starting in NORMAL mode..."
  gradle clean bootRun
fi

