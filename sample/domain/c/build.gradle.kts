plugins {
    `java-library`
}

dependencies {
    implementation(project(":feature:c"))
    implementation(project(":legacy:c"))
}