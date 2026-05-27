#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
IMAGE_NAME="standbase-backend"
CONTAINER_NAME="standbase-backend"
ENV_FILE="$SCRIPT_DIR/.env"
LOGS_DIR="$SCRIPT_DIR/logs"
START_TS="$(date +%Y%m%d_%H%M%S)"
LOG_FILE="$LOGS_DIR/standbase-backend${START_TS}.log"

if [ ! -f "$ENV_FILE" ]; then
  echo "Error: .env file not found at $ENV_FILE"
  exit 1
fi

echo "Building image $IMAGE_NAME..."
podman build -t "$IMAGE_NAME" "$SCRIPT_DIR"

# Remove any existing stopped/running container with this name
if podman container exists "$CONTAINER_NAME" 2>/dev/null; then
  echo "Removing existing container $CONTAINER_NAME..."
  podman rm -f "$CONTAINER_NAME"
fi

echo "Starting container $CONTAINER_NAME..."
podman run -d \
  --name "$CONTAINER_NAME" \
  --env-file "$ENV_FILE" \
  -v "$ENV_FILE:/deployments/.env:ro,Z" \
  -p 5554:5554 \
  "$IMAGE_NAME"

mkdir -p "$LOGS_DIR"
podman logs -f "$CONTAINER_NAME" >> "$LOG_FILE" 2>&1 &
echo "Container started. Logging to $LOG_FILE"
