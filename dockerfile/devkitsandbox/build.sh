#!/usr/bin/env bash
set -euo pipefail

REGISTRY=swr.cn-south-1.myhuaweicloud.com
ORG=cloud_devstage
REPO=devkitsandbox
TAG=$(date +%Y%m%d%H%M%S%3N)
IMAGE="${REGISTRY}/${ORG}/${REPO}:${TAG}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

echo "Building ${IMAGE} ..."
docker build -t "${IMAGE}" "${SCRIPT_DIR}"
echo "Built: ${IMAGE}"
