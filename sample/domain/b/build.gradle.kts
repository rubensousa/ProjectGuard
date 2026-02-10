plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:b"))
    implementation(project(":legacy:b"))
}