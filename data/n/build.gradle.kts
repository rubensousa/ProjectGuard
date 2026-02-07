plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:n"))
    testImplementation(libs.mockk)
}