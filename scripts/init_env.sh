set -e

set -a
source <(grep -v '^#' .env | tr -d '\r' | xargs -d '\r\n')
set +a
