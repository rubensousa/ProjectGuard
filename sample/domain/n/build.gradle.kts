plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:n"))
    implementation(project(":legacy:n"))
    testImplementation(libs.mockk)
}