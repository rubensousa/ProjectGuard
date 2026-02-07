plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:j"))
    testImplementation(libs.mockk)
}