plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:x"))
    implementation(project(":legacy:x"))
    testImplementation(libs.mockk)
}