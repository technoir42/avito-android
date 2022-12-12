set -xeu

# Removes docker container and kills all child processes
trap "trap - SIGTERM && docker rm --force emcee-queue && kill -- -$$" SIGINT SIGTERM EXIT ERR

echo "Starting queue..."
docker run --rm --network="host" --name="emcee-queue" $DOCKER_REGISTRY/android/emcee-queue:ffd97d9356aa >/dev/null 2>&1 &
echo "Queue started"

echo "Building worker..."
./gradlew :subprojects:emcee:worker:build -x test --quiet >/dev/null

echo "Starting worker..."
java -jar ./subprojects/emcee/worker/build/libs/emcee-worker.jar start -c ./subprojects/emcee/worker/config.json -ll info