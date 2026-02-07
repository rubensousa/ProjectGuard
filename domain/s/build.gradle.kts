plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:s"))
    implementation(project(":legacy:s"))
    testImplementation(libs.mockk)
}