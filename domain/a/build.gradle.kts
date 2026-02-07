plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:a"))
    implementation(project(":legacy:a"))
}