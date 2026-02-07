plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:y"))
    testImplementation(libs.mockk)
}