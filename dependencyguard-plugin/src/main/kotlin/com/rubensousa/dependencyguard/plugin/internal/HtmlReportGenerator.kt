/*
 * Copyright 2026 Rúben Sousa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.rubensousa.dependencyguard.plugin.internal

import java.util.Locale

internal class HtmlReportGenerator {

    fun generate(report: DependencyGuardReport): String {
        val fatalModules = report.modules.filter { it.fatal.isNotEmpty() }
        val suppressedModules = report.modules.filter { it.suppressed.isNotEmpty() }
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
            ${generateSidebar(fatalModules, suppressedModules)}
            ${generateMainContent(report, fatalModules, suppressedModules)}
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

    private fun generateSidebar(
        fatalModules: List<ModuleReport>,
        suppressedModules: List<ModuleReport>,
    ): String {
        val fatalGroups = fatalModules.groupBy { getGroupName(it.module) }.toSortedMap()
        val suppressedGroups = suppressedModules.groupBy { getGroupName(it.module) }.toSortedMap()

        return """
        <div class="sidebar">
            ${generateToc("Fatal Matches", fatalGroups)}
            ${generateToc("Suppressed Matches", suppressedGroups, isSuppressed = true)}
        </div>
        """.trimIndent()
    }

    private fun generateToc(
        title: String,
        groups: Map<String, List<ModuleReport>>,
        isSuppressed: Boolean = false,
    ): String {
        if (groups.isEmpty()) return ""
        val anchorPrefix = if (isSuppressed) "suppressed" else "fatal"
        val tocHtml = groups.map { (groupName, modules) ->
            val groupAnchorId = "$anchorPrefix-${groupName.replace(":", "").replace(" ", "-").lowercase(Locale.getDefault())}"
            val count = if (isSuppressed) {
                modules.sumOf { it.suppressed.size }
            } else {
                modules.sumOf { it.fatal.size }
            }
            val badgeClass = if (isSuppressed) "suppressed" else "fatal"

            val childModulesHtml = modules.sortedBy { it.module }.joinToString("\n") { moduleReport ->
                val childCount = if (isSuppressed) moduleReport.suppressed.size else moduleReport.fatal.size
                if (childCount == 0) "" else {
                    val moduleAnchorId = "$anchorPrefix-module-${moduleReport.module.substring(1).replace(":", "-")}"
                    val childBadgeClass = if (isSuppressed) "suppressed-light" else "fatal-light"
                    val moduleName = moduleReport.module.split(':').last()
                    """
                    <li>
                        <a href="#$moduleAnchorId">
                            <span>$moduleName</span>
                            <span class="badge $childBadgeClass">$childCount</span>
                        </a>
                    </li>
                    """
                }
            }

            """
            <details>
                <summary>
                    <a href="#$groupAnchorId">$groupName</a>
                    <span class="badge $badgeClass">$count</span>
                </summary>
                ${if (childModulesHtml.isNotBlank()) "<ul>\n$childModulesHtml\n</ul>" else ""}
            </details>
            """
        }.joinToString("\n")

        return """
        <div class="toc-section">
            <h2>$title</h2>
            <nav>
                $tocHtml
            </nav>
        </div>
        """.trimIndent()
    }

    private fun generateMainContent(
        report: DependencyGuardReport,
        fatalModules: List<ModuleReport>,
        suppressedModules: List<ModuleReport>,
    ): String {
        val totalFatalMatches = fatalModules.sumOf { it.fatal.size }
        val fatalContent = generateSectionContent("Fatal Matches", fatalModules)
        val suppressedContent =
            generateSectionContent("Suppressed Matches", suppressedModules, isSuppressed = true)

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
                    $suppressedContent
                </main>
            </div>
        </div>
        """.trimIndent()
    }

    private fun generateSectionContent(
        title: String,
        modules: List<ModuleReport>,
        isSuppressed: Boolean = false,
    ): String {
        if (modules.isEmpty()) return ""
        val groupedModules = modules.groupBy { getGroupName(it.module) }.toSortedMap()
        val groupHtml = groupedModules.map { (groupName, moduleList) ->
            generateModuleGroup(groupName, moduleList, isSuppressed)
        }.joinToString("\n")
        val titleClass = if (isSuppressed) "suppressed-title" else "fatal-title"
        return """
            <section>
                <h2 class="$titleClass">$title</h2>
                $groupHtml
            </section>
        """
    }


    private fun generateModuleGroup(
        groupName: String,
        modules: List<ModuleReport>,
        isSuppressed: Boolean,
    ): String {
        val anchorPrefix = if (isSuppressed) "suppressed" else "fatal"
        val anchorId = "$anchorPrefix-${groupName.replace(":", "").replace(" ", "-").lowercase(Locale.getDefault())}"
        val moduleDetailsHtml = modules.joinToString("\n") { moduleReport ->
            generateModuleDetails(moduleReport, isSuppressed)
        }
        return """
        <div class="group-container" id="$anchorId">
            <h3>$groupName</h3>
            $moduleDetailsHtml
        </div>
        """.trimIndent()
    }

    private fun generateModuleDetails(moduleReport: ModuleReport, isSuppressed: Boolean): String {
        val anchorPrefix = if (isSuppressed) "suppressed" else "fatal"
        val moduleAnchorId = "$anchorPrefix-module-${moduleReport.module.substring(1).replace(":", "-")}"
        val table = if(isSuppressed) {
            generateSuppressedTable(moduleReport.suppressed)
        } else {
            generateFatalTable(moduleReport.fatal)
        }
        return """
        <details class="module" id="$moduleAnchorId" open>
            <summary>
                <h4>${moduleReport.module}</h4>
                <div>
                    ${if (moduleReport.fatal.isNotEmpty()) """<span class="badge fatal">${moduleReport.fatal.size} fatal</span>""" else ""}
                    ${if (moduleReport.suppressed.isNotEmpty()) """<span class="badge suppressed">${moduleReport.suppressed.size} suppressed</span>""" else ""}
                </div>
            </summary>
            $table
        </details>
        """.trimIndent()
    }

    private fun generateFatalTable(
        matches: List<FatalMatch>,
    ): String {
        if (matches.isEmpty()) return ""
        val reasonHeader = "Restriction Reason"
        val tableRows = matches.joinToString("\n") { match ->
            val reason = match.reason
            """
            <tr>
                <td><code>${match.pathToDependency}</code></td>
                <td>${reason}</td>
            </tr>
            """.trimIndent()
        }
        return """
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Dependency</th>
                        <th>$reasonHeader</th>
                    </tr>
                </thead>
                <tbody>
                    $tableRows
                </tbody>
            </table>
        </div>
        """.trimIndent()
    }

    private fun generateSuppressedTable(
        matches: List<SuppressedMatch>,
    ): String {
        if (matches.isEmpty()) return ""
        val reasonHeader = "Suppression Reason"
        val tableRows = matches.joinToString("\n") { match ->
            val reason =match.suppressionReason
            """
            <tr>
                <td><code>${match.pathToDependency}</code></td>
                <td>${reason}</td>
            </tr>
            """.trimIndent()
        }
        return """
        <div class="table-container">
            <table>
                <thead>
                    <tr>
                        <th>Dependency</th>
                        <th>$reasonHeader</th>
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
            --color-suppressed: #6c757d;
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
        .sidebar nav > details {
            margin-bottom: 0.5rem;
        }
        .sidebar nav > details > summary {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.5rem 1rem;
            border-radius: 6px;
            font-weight: 500;
            list-style: none; /* Hide the default disclosure triangle */
            cursor: pointer;
        }
        .sidebar nav > details > summary::-webkit-details-marker {
            display: none; /* Hide the default disclosure triangle for Chrome/Safari */
        }
        .sidebar nav > details > summary:hover {
            background-color: #f1f3f5;
            color: var(--color-text-primary);
        }
        .sidebar nav > details > summary:hover a {
            color: var(--color-text-primary);
        }
        .sidebar ul {
            list-style: none;
            padding: 0;
            margin: 0;
        }
        .sidebar a {
            color: var(--color-text-secondary);
            text-decoration: none;
        }
        .sidebar details > ul {
            padding-left: 1rem;
            margin-top: 0.5rem;
        }
        .sidebar details > ul a {
            display: flex;
            justify-content: space-between;
            align-items: center;
            padding: 0.25rem 1rem;
            font-weight: 400;
            border-radius: 6px;
        }
        .sidebar details > ul a:hover {
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
        .suppressed-title { color: var(--color-suppressed); }
        
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
        .module > summary {
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
        .badge.suppressed { background-color: var(--color-suppressed); }
        .badge.fatal-light {
            background-color: #fce8e6;
            color: var(--color-fatal);
            border: 1px solid var(--color-fatal);
        }
        .badge.suppressed-light {
            background-color: #e8eaed;
            color: var(--color-text-primary);
            border: 1px solid var(--color-suppressed);
        }
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
