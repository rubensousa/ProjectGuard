plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:i"))
    implementation(project(":legacy:i"))
    testImplementation(libs.mockk)
}