plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:v"))
    implementation(project(":legacy:v"))
}