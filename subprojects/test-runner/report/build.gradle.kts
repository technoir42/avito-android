plugins {
    id("convention.kotlin-jvm")
    id("convention.publish-kotlin-library")
    id("convention.libraries")
    id("convention.test-fixtures")
}

publish {
    artifactId.set("runner-report")
}

dependencies {
    api(project(":common:report-viewer"))
    api(project(":common:report-api"))

    implementation(project(":common:time"))
    implementation(project(":common:http-client"))

    testImplementation(testFixtures(project(":common:report-api")))

    testFixturesImplementation(testFixtures(project(":common:logger")))
    testFixturesImplementation(testFixtures(project(":common:time")))
    testFixturesImplementation(testFixtures(project(":common:report-viewer")))
}

kotlin {
    explicitApi()
}