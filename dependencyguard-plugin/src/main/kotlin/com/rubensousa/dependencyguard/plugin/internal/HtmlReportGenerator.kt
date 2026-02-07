package com.rubensousa.dependencyguard.plugin.internal

import java.util.Locale

internal class HtmlReportGenerator {

    fun generate(report: DependencyGuardReport): String {
        val fatalModules = report.modules.filter { it.fatalMatches.isNotEmpty() }
        val excludedModules = report.modules.filter { it.excludedMatches.isNotEmpty() }
        return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>DependencyGuard Report</title>
            <style>
            ${generateCss()}
            </style>
        </head>
        <body>
            ${generateSidebar(fatalModules, excludedModules)}
            ${generateMainContent(report, fatalModules, excludedModules)}
        </body>
        </html>
        """.trimIndent()
    }

    private fun getGroupName(modulePath: String): String {
        val parts = modulePath.split(":")
        if (parts.size > 2) {
            return ":${parts[1]}"
        }
        return "Top-level Modules"
    }

    private fun generateSidebar(fatalModules: List<ModuleReport>, excludedModules: List<ModuleReport>): String {
        val fatalGroups = fatalModules.groupBy { getGroupName(it.module) }.toSortedMap()
        val excludedGroups = excludedModules.groupBy { getGroupName(it.module) }.toSortedMap()

        return """
        <div class="sidebar">
            ${generateToc("Fatal Matches", fatalGroups)}
            ${generateToc("Excluded Matches", excludedGroups, isExcluded = true)}
        </div>
        """.trimIndent()
    }

    private fun generateToc(title: String, groups: Map<String, List<ModuleReport>>, isExcluded: Boolean = false): String {
        if (groups.isEmpty()) return ""
        val anchorPrefix = if (isExcluded) "excluded" else "fatal"
        val tocHtml = groups.map { (groupName, modules) ->
            val anchorId = "$anchorPrefix-${groupName.replace(":", "").replace(" ", "-").lowercase(Locale.getDefault())}"
            val count = if (isExcluded) {
                modules.sumOf { it.excludedMatches.size }
            } else {
                modules.sumOf { it.fatalMatches.size }
            }
            val badgeClass = if(isExcluded) "excluded" else "fatal"
            
            """
            <li>
                <a href="#$anchorId">
                    <span>$groupName</span>
                    <span class="badge $badgeClass">$count</span>
                </a>
            </li>
            """
        }.joinToString("\n")

        return """
        <div class="toc-section">
            <h2>$title</h2>
            <nav>
                <ul>
                    $tocHtml
                </ul>
            </nav>
        </div>
        """.trimIndent()
    }

    private fun generateMainContent(
        report: DependencyGuardReport,
        fatalModules: List<ModuleReport>,
        excludedModules: List<ModuleReport>
    ): String {
        val totalFatalMatches = fatalModules.sumOf { it.fatalMatches.size }
        val fatalContent = generateSectionContent("Fatal Matches", fatalModules)
        val excludedContent = generateSectionContent("Excluded Matches", excludedModules, isExcluded = true)

        return """
        <div class="main-content">
            <div class="container">
                <header>
                    <h1>DependencyGuard Report</h1>
                    <p style="color: var(--color-text-secondary); margin: 0.25rem 0 0 0;">
                        Found $totalFatalMatches fatal matches across ${report.modules.size} modules.
                    </p>
                </header>
                <main>
                    $fatalContent
                    $excludedContent
                </main>
            </div>
        </div>
        """.trimIndent()
    }

    private fun generateSectionContent(title: String, modules: List<ModuleReport>, isExcluded: Boolean = false): String {
        if (modules.isEmpty()) return ""
        val groupedModules = modules.groupBy { getGroupName(it.module) }.toSortedMap()
        val groupHtml = groupedModules.map { (groupName, moduleList) ->
            generateModuleGroup(groupName, moduleList, isExcluded)
        }.joinToString("\n")
        val titleClass = if(isExcluded) "excluded-title" else "fatal-title"
        return """
            <section>
                <h2 class="$titleClass">$title</h2>
                $groupHtml
            </section>
        """
    }


    private fun generateModuleGroup(groupName: String, modules: List<ModuleReport>, isExcluded: Boolean): String {
        val anchorPrefix = if (isExcluded) "excluded" else "fatal"
        val anchorId = "$anchorPrefix-${groupName.replace(":", "").replace(" ", "-").lowercase(Locale.getDefault())}"
        val moduleDetailsHtml = modules.joinToString("\n") { moduleReport ->
            generateModuleDetails(moduleReport)
        }
        return """
        <div class="group-container" id="$anchorId">
            <h3>$groupName</h3>
            $moduleDetailsHtml
        </div>
        """.trimIndent()
    }

    private fun generateModuleDetails(moduleReport: ModuleReport): String {
        val fatalMatches = generateTable(moduleReport.fatalMatches)
        val excludedMatchesHtml = generateTable(moduleReport.excludedMatches, isExcluded = true)

        return """
        <details class="module" open>
            <summary>
                <h4>${moduleReport.module}</h4>
                <div>
                    ${if (moduleReport.fatalMatches.isNotEmpty()) """<span class="badge fatal">${moduleReport.fatalMatches.size} fatal</span>""" else ""}
                    ${if (moduleReport.excludedMatches.isNotEmpty()) """<span class="badge excluded">${moduleReport.excludedMatches.size} excluded</span>""" else ""}
                </div>
            </summary>
            $fatalMatches
            $excludedMatchesHtml
        </details>
        """.trimIndent()
    }

    private fun generateTable(matches: List<Match>, isExcluded: Boolean = false): String {
        if (matches.isEmpty()) return ""
        val tableRows = matches.joinToString("\n") { match ->
            """
            <tr>
                <td><code>${match.dependency}</code></td>
                <td>${match.reason}</td>
            </tr>
            """.trimIndent()
        }
        return """
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Dependency</th>
                        <th>Reason</th>
                    </tr>
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>
        </div>
        """.trimIndent()
    }

    private fun generateCss(): String {
        return """
        :root {
            --color-background: #f8f9fa;
            --color-surface: #ffffff;
            --color-fatal: #d32f2f;
            --color-excluded: #6c757d;
            --color-text-primary: #212529;
            --color-text-secondary: #6c757d;
            --color-border: #dee2e6;
            --font-family-sans: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            --border-radius: 8px;
            --sidebar-width: 280px;
        }
        body {
            font-family: var(--font-family-sans);
            margin: 0;
            background-color: var(--color-background);
            color: var(--color-text-primary);
            display: flex;
        }
        .sidebar {
            width: var(--sidebar-width);
            position: fixed;
            top: 0;
            left: 0;
            height: 100vh;
            background-color: var(--color-surface);
            border-right: 1px solid var(--color-border);
            padding: 1.5rem;
            overflow-y: auto;
            box-sizing: border-box;
        }
        .toc-section { margin-bottom: 2rem; }
        .sidebar h2 {
            margin: 0 0 1rem 0;
            font-size: 1.1rem;
            color: var(--color-text-primary);
            border-bottom: 1px solid var(--color-border);
            padding-bottom: 0.5rem;
        }
        .sidebar ul {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        .sidebar ul a {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.5rem 1rem;
            color: var(--color-text-secondary);
            text-decoration: none;
            border-radius: 6px;
            font-weight: 500;
        }
        .sidebar ul a:hover {
            background-color: #f1f3f5;
            color: var(--color-text-primary);
        }
        .main-content {
            margin-left: var(--sidebar-width);
            flex-grow: 1;
        }
        .container {
            max-width: 1000px;
            margin: 2rem auto;
            padding: 2rem;
        }
        header {
            border-bottom: 1px solid var(--color-border);
            padding-bottom: 1.5rem;
            margin-bottom: 2rem;
        }
        h1 {
            color: var(--color-fatal);
            margin: 0;
        }
        section {
            background-color: var(--color-surface);
            border: 1px solid var(--color-border);
            border-radius: var(--border-radius);
            margin-bottom: 2rem;
            padding: 1.5rem;
        }
        section > h2 {
            font-size: 1.75rem;
            margin-bottom: 1.5rem;
        }
        .fatal-title { color: var(--color-fatal); }
        .excluded-title { color: var(--color-excluded); }
        
        .group-container { margin-bottom: 2rem; }
        .group-container:last-child { margin-bottom: 0; }
        .group-container > h3 {
            font-size: 1.25rem;
            margin-bottom: 1.5rem;
            padding-bottom: 0.5rem;
            border-bottom: 1px solid var(--color-border);
        }
        .module {
            border: 1px solid var(--color-border);
            border-radius: var(--border-radius);
            margin-bottom: 1.5rem;
            overflow: hidden;
        }
        summary {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 1rem 1.5rem;
            background-color: #f8f9fa;
            cursor: pointer;
            outline: none;
        }
        .badge {
            color: white;
            padding: 0.35em 0.65em;
            font-size: .85em;
            font-weight: 600;
            border-radius: 50rem;
            margin-left: 0.5rem;
        }
        .badge.fatal { background-color: var(--color-fatal); }
        .badge.excluded { background-color: var(--color-excluded); }
        .table-container { padding: 0 1.5rem 1rem; }
        table {
            width: 100%;
            border-collapse: collapse;
            table-layout: fixed;
        }
        th, td {
            text-align: left;
            padding: 0.75rem 0;
            border-bottom: 1px solid var(--color-border);
            word-wrap: break-word;
        }
        thead th {
            padding: 0.75rem 0;
        }
        thead th:first-child, thead th:last-child {
            width: 50%;
        }
        tbody tr:last-child td {
            border-bottom: none;
        }
        """
    }
}
