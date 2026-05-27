#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="standbase-backend"
IMAGE_NAME="standbase-backend"

echo "Stopping container $CONTAINER_NAME..."
podman stop "$CONTAINER_NAME" 2>/dev/null || true

echo "Removing container $CONTAINER_NAME..."
podman rm "$CONTAINER_NAME" 2>/dev/null || true

echo "Removing image $IMAGE_NAME..."
podman rmi "$IMAGE_NAME" 2>/dev/null || true

echo "Done."
