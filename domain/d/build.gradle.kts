plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:d"))
    implementation(project(":legacy:d"))
    testImplementation(libs.mockk)
}