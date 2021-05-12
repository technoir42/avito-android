plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    id("convention.libraries")
}

publish {
    artifactId.set("runner-service")
}

dependencies {
    api(project(":common:coroutines-extension"))
    api(project(":common:statsd"))
    api(project(":common:time"))
    implementation(project(":test-runner:shared"))
    implementation(project(":common:result"))
    implementation(project(":common:test-report-artifacts")) {
        because("DeviceWorker pulls test artifacts")
    }
    implementation(libs.ddmlib)
    implementation(libs.rxJava)

    testImplementation(testFixtures(project(":common:logger")))
    testImplementation(testFixtures(project(":common:time")))
    testImplementation(project(":common:files"))
    testImplementation(project(":common:truth-extensions"))
    testImplementation(project(":common:resources"))
    testImplementation(project(":gradle:test-project"))
    testImplementation(project(":test-runner:shared-test"))
    testImplementation(libs.kotlinReflect)
    testImplementation(libs.mockitoKotlin)
    testImplementation(libs.mockitoJUnitJupiter)
    testImplementation(libs.coroutinesTest)
}