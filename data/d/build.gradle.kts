plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:d"))
    testImplementation(libs.mockk)
}