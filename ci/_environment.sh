#!/usr/bin/env bash

set -e

if [[ -z "${DOCKER_REGISTRY+x}" ]]; then
    echo "ERROR: DOCKER_REGISTRY env must be set"
    exit 1
fi

if [[ -z "${ARTIFACTORY_URL+x}" ]]; then
    echo "WARN: ARTIFACTORY_URL env is not set. It's required for hermetic builds."
fi

IMAGE_ANDROID_BUILDER=${DOCKER_REGISTRY}/android/builder-hermetic:78633b8380a9
IMAGE_BUILDER=${DOCKER_REGISTRY}/android/image-builder:9502228da1d3
DOCUMENTATION_IMAGE=${DOCKER_REGISTRY}/android/documentation:802502572f
