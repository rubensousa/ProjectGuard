plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:i"))
    testImplementation(libs.mockk)
}