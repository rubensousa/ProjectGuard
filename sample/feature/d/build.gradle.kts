plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:e"))
    testImplementation(libs.mockk)
}