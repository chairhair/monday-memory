#!/usr/bin/env bash
# run.sh — robust runner for Spring Boot via Gradle with .env loading on WSL/Linux
set -Eeuo pipefail

# --- config defaults ---
ENV_FILE="${ENV_FILE:-.env}"    # allow override: ENV_FILE=path/to/other.env ./run.sh
GRADLE_TASKS=("clean" "bootRun") # default tasks

usage() {
  cat <<'USAGE'
Usage: ./run.sh [options] [-- <extra gradle args>]

Options:
  -e <file>     Path to .env file (default: ./.env or ENV_FILE env var)
  -p <profile>  Set profile (exports APP_ENV=<profile> and SPRING_PROFILES_ACTIVE=<profile>)
  -s            Use Spring var only (export SPRING_PROFILES_ACTIVE; don't set APP_ENV)
  -n            Do NOT stop daemon (skip './gradlew --stop')
  -h            Show this help

Examples:
  ./run.sh
  ./run.sh -p dev
  ./run.sh -e .env.local -p staging -- -Ddebug
  ENV_FILE=.env.docker ./run.sh -p prod
USAGE
}

STOP_DAEMON=1
SPRING_ONLY=0
PROFILE_ARG=""

# --- parse args ---
while getopts ":e:p:snh" opt; do
  case "$opt" in
    e) ENV_FILE="$OPTARG" ;;
    p) PROFILE_ARG="$OPTARG" ;;
    s) SPRING_ONLY=1 ;;
    n) STOP_DAEMON=0 ;;
    h) usage; exit 0 ;;
    \?) echo "Unknown option: -$OPTARG" >&2; usage; exit 2 ;;
    :) echo "Missing arg for -$OPTARG" >&2; usage; exit 2 ;;
  esac
done
shift $((OPTIND-1))

# Any args after "--" go straight to Gradle; otherwise default tasks used.
if [[ "${1:-}" == "--" ]]; then
  shift
  GRADLE_TASKS=("$@")
elif [[ "$#" -gt 0 ]]; then
  GRADLE_TASKS=("$@")
fi

# --- helpers ---
normalize_env_file() {
  local f="$1"
  [[ -f "$f" ]] || return 0
  # Fix CRLF if the file was edited on Windows
  if command -v dos2unix >/dev/null 2>&1; then
    dos2unix -q "$f" || true
  else
    sed -i 's/\r$//' "$f" || true
  fi
}

load_env_file() {
  local f="$1"
  [[ -f "$f" ]] || return 0
  # Export everything the file defines
  set -a
  # shellcheck disable=SC1090
  source "$f"
  set +a
}

print_env_summary() {
  echo ">> Environment visible to this shell:"
  printf "   APP_ENV=%s\n" "${APP_ENV-<unset>}"
  printf "   SPRING_PROFILES_ACTIVE=%s\n" "${SPRING_PROFILES_ACTIVE-<unset>}"
}

ensure_in_wsl_or_linux() {
  # Not strictly required, but warn if running from Windows shell
  if command -v uname >/dev/null 2>&1; then
    local u
    u="$(uname -a || true)"
    if [[ "$u" =~ MINGW|MSYS|CYGWIN ]]; then
      echo "⚠️  You appear to be in a Windows shell. Run this inside WSL/Linux for env inheritance." >&2
    fi
  fi
}

# --- main ---
ensure_in_wsl_or_linux

# Normalize + load .env
if [[ -f "$ENV_FILE" ]]; then
  echo ">> Loading env from: $ENV_FILE"
  normalize_env_file "$ENV_FILE"
  load_env_file "$ENV_FILE"
else
  echo ">> No env file found at: $ENV_FILE (skipping)"
fi

# Apply -p profile override if provided
if [[ -n "$PROFILE_ARG" ]]; then
  if (( SPRING_ONLY == 1 )); then
    export SPRING_PROFILES_ACTIVE="$PROFILE_ARG"
  else
    export APP_ENV="$PROFILE_ARG"
    export SPRING_PROFILES_ACTIVE="${SPRING_PROFILES_ACTIVE:-$PROFILE_ARG}"
  fi
fi

print_env_summary

# Kill stale Gradle daemons so env changes are picked up
if (( STOP_DAEMON == 1 )); then
  echo ">> Stopping Gradle daemons..."
  ./gradlew --stop >/dev/null 2>&1 || true
fi

# Confirm Gradle sees vars (optional quick check)
echo ">> Verifying environment via Gradle (printEnv task is optional)..."
./gradlew --no-daemon -q -Dorg.gradle.warning.mode=none \
  -PverifyEnvPlaceholder=true \
  help >/dev/null 2>&1 || true

# Run tasks
echo ">> Running: ./gradlew --no-daemon ${GRADLE_TASKS[*]}"
exec ./gradlew --no-daemon "${GRADLE_TASKS[@]}"

