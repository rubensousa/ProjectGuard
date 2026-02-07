plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:s"))
    testImplementation(libs.mockk)
}