plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:f"))
    implementation(project(":legacy:f"))
}