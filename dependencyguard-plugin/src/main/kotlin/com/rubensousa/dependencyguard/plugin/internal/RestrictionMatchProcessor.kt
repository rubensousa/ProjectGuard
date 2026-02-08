package com.rubensousa.dependencyguard.plugin.internal

internal class RestrictionMatchProcessor {

    fun process(
        matches: List<RestrictionMatch>,
    ): List<RestrictionMatch> {
        val output = mutableListOf<RestrictionMatch>()
        val visitedLinks = mutableSetOf<DependencyLink>()
        matches.forEach { match ->
            val link = DependencyLink(
                module = match.modulePath,
                dependency = match.dependencyPath
            )
            if (!visitedLinks.contains(link)) {
                visitedLinks.add(link)
                output.add(match)
            }
        }
        return output
    }

}
