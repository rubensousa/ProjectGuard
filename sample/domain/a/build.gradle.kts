plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":feature:a"))
    implementation(project(":legacy:a"))
    testFixturesImplementation(project(":feature:a"))
}