plugins {
    `java-library`
    `java-test-fixtures`
}

dependencies {
    implementation(project(":data:a"))
    testFixturesImplementation(project(":feature:a"))
}