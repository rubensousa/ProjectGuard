plugins {
    `java-library`
}

dependencies {
    implementation(project(":legacy:b"))
    testImplementation(libs.mockk)
}