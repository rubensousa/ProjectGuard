plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:o"))
    testImplementation(libs.mockk)
}