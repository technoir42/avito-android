package ru.avito.image_builder.internal.command.emulator

import ru.avito.image_builder.internal.docker.ContainerId
import ru.avito.image_builder.internal.docker.Docker
import ru.avito.image_builder.internal.docker.ImageId
import java.util.logging.Logger

internal class EmulatorPreparer(
    private val docker: Docker,
    private val emulatorTester: EmulatorTester,
) {

    private val log: Logger = Logger.getLogger(this::class.java.simpleName)

    fun prepareEmulators(imageId: ImageId, apis: Set<Int>): ImageId {
        val containerId = runContainer(imageId)

        for (api in apis) {
            prepareEmulator(containerId, api)
        }

        val preparedImageId = commitChanges(containerId)

        removeContainer(containerId)

        emulatorTester.testEmulator(preparedImageId)

        return preparedImageId
    }

    private fun runContainer(imageId: ImageId): ContainerId {
        val runResult = docker.run(
            "--rm",
            "-d",
            "--device", "/dev/kvm",
            "--entrypoint", "sleep",
            imageId.value,
            "1h"
        )
        check(runResult.isSuccess) {
            "Failed to run the image: ${runResult.exceptionOrNull()}"
        }
        val containerId = runResult.getOrThrow()
        log.info("Run container $containerId")
        return ContainerId(containerId)
    }

    private fun prepareEmulator(containerId: ContainerId, api: Int) {
        val result = docker.exec(containerId.value, "bash", "-c", "VERSION=$api ./prepare_snapshot.sh")
        check(result.isSuccess) {
            "Failed to exec preparation script: ${result.exceptionOrNull()}"
        }
        val output = result.getOrThrow()
        check(output.contains("Emulator preparation finished")) {
            """
                |Couldn't find message about finished preparation. 
                |Last messages: ${output.takeLast(2048)}
            """.trimMargin()
        }
    }

    private fun commitChanges(containerId: ContainerId): ImageId {
        log.info("Committing changed container ...")
        val commitResult = docker.commit(
            change = "ENTRYPOINT [\"./entrypoint.sh\"]",
            container = containerId.value
        )
        check(commitResult.isSuccess) {
            "Failed to commit container: ${commitResult.exceptionOrNull()}"
        }
        val imageId = commitResult.getOrThrow()
        log.info("Committed image: ${imageId.value}")
        return imageId
    }

    private fun removeContainer(containerId: ContainerId) {
        val result = docker.remove("--force", container = containerId.value)
        check(result.isSuccess) {
            "Failed to remove container ${containerId.value}: ${result.exceptionOrNull()}"
        }
    }
}