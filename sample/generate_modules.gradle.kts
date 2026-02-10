val moduleGroups = listOf("domain", "data", "feature", "legacy")
val subModules = 'a'..'z'
val root = rootDir

for (group in moduleGroups) {
    for (sub in subModules) {
        val moduleDir = File(root, "$group/$sub")
        val modulePath = ":$group:$sub"

        // Ensure the directory exists and create a basic build file
        if (!moduleDir.exists()) {
            moduleDir.mkdirs()
        }

        val dependencies = mutableListOf<String>()
        when (group) {
            "domain" -> {
                // Violates restrict(":domain") -> deny(":feature")
                dependencies.add("implementation(project(\":sample:feature:$sub\"))")
                // Violates restrictAll -> deny(":legacy")
                dependencies.add("implementation(project(\":sample:legacy:$sub\"))")
            }
            "data" -> {
                // Violates restrict(":data") -> deny(":feature")
                dependencies.add("implementation(project(\":sample:feature:$sub\"))")
            }
            "feature" -> {
                // Violates restrict(":feature") -> deny(":feature")
                // Make 'c' depend on 'd', 'e' on 'f', etc. to create violations
                val nextSub = if (sub < 'z') sub + 1 else 'a'
                dependencies.add("implementation(project(\":sample:feature:$nextSub\"))")
            }
        }
        
        // Add a mockk violation for every 5th module
        if (sub.code % 5 == 0) {
            dependencies.add("testImplementation(libs.mockk)")
        }

        File(moduleDir, "build.gradle.kts").writeText("""
            plugins {
                `java-library`
            }
            
            dependencies {
                ${dependencies.joinToString("\n                ")}
            }
        """.trimIndent())

        File(moduleDir, ".gitignore").writeText("/build")
    }
}

// Update settings.gradle.kts if needed
val settingsFile = File(root, "settings.gradle.kts")
val settingsContent = settingsFile.readText()
val newIncludes = moduleGroups.flatMap { group ->
    subModules.map { sub ->
        val modulePath = ":$group:$sub"
        if (!settingsContent.contains("\"$modulePath\"")) {
            "include(\"$modulePath\")"
        } else {
            null
        }
    }
}.filterNotNull()

if (newIncludes.isNotEmpty()) {
    settingsFile.appendText("\n" + newIncludes.joinToString("\n"))
    println("Added ${newIncludes.size} new modules to settings.gradle.kts")
} else {
    println("All modules already exist in settings.gradle.kts")
}
