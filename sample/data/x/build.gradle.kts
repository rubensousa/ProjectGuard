plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:x"))
    testImplementation(libs.mockk)
}