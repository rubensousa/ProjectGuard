plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:t"))
    testImplementation(libs.mockk)
}