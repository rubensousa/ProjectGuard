// The report data is injected by the report generator
document.addEventListener('DOMContentLoaded', function() {
    if (window.REPORT_DATA) {
        initializeReport(window.REPORT_DATA);
    } else {
        document.body.innerHTML = "<h1>Error: Report data not found.</h1>";
    }
});

function initializeReport(report) {
    renderSummaryStats(report);
    setupTabListeners(report);
    renderForActiveTab(report);
    setupGlobalEventListeners();
}

function renderSummaryStats(report) {
    const fatalCount = report.modules.reduce((sum, module) => sum + module.fatal.length, 0);
    const suppressedCount = report.modules.reduce((sum, module) => sum + module.suppressed.length, 0);
    document.getElementById('total-fatal-matches').textContent = fatalCount;
    document.getElementById('total-suppressed-matches').textContent = suppressedCount;
}

function setupTabListeners(report) {
    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', () => {
            const currentActiveButton = document.querySelector('.tab-button.active');
            if (currentActiveButton) currentActiveButton.classList.remove('active');

            const currentActiveTab = document.querySelector('.tab-content.active');
            if (currentActiveTab) currentActiveTab.classList.remove('active');

            button.classList.add('active');
            document.getElementById(button.dataset.tab).classList.add('active');

            updateViewControlsHeader(button.dataset.tab);
            renderForActiveTab(report);
        });
    });
}

function updateViewControlsHeader(activeTab) {
    const header = document.getElementById('view-title');
    if (activeTab === 'by-module') {
        header.textContent = 'MODULE RESTRICTIONS';
    } else {
        header.textContent = 'DEPENDENCY RESTRICTIONS';
    }
}

function renderForActiveTab(report) {
    const activeTabId = document.querySelector('.tab-button.active').dataset.tab;
    if (activeTabId === 'by-module') {
        renderSidebarForModules(report);
        renderByModuleView(report);
    } else {
        renderSidebarForDependencies(report);
        renderByDependencyView(report);
    }
}

function setupGlobalEventListeners() {
    document.body.addEventListener('click', function(event) {
        // Handle main card header clicks for expansion
        const cardHeader = event.target.closest('.card-header');
        if (cardHeader) {
            const cardBody = cardHeader.nextElementSibling;
            if (cardBody && cardBody.classList.contains('card-body')) {
                cardHeader.classList.toggle('expanded');
                cardBody.style.display = cardBody.style.display === 'block' ? 'none' : 'block';
            }
        }
    });

    document.getElementById('sidebar').addEventListener('click', function(event) {
        const summary = event.target.closest('summary');
        if (summary) {
            event.preventDefault();
            const details = summary.parentElement;
            details.open = !details.open;
        }
    });
}


// --- Sidebar Rendering ---
function groupModules(modules) {
    const grouped = {};
    modules.forEach(module => {
        const parts = module.module.split(':');
        let groupName = 'ungrouped';
        if (parts.length > 2) {
            groupName = parts.slice(0, 2).join(':');
        }
        if (!grouped[groupName]) {
            grouped[groupName] = [];
        }
        grouped[groupName].push(module);
    });
    return grouped;
}

function renderModuleGroups(groupedModules, showFatalCount) {
    let content = '<ul>';
    Object.keys(groupedModules).sort().forEach(groupName => {
        const modules = groupedModules[groupName];
        if (groupName === 'ungrouped') {
            modules.forEach(module => {
                content += `<li><a href="#module-${module.module.replace(/:/g, '-')}">
                    <span>${module.module}</span>
                    ${showFatalCount ? `<span class="badge badge-fatal">${module.fatal.length}</span>` : ''}
                </a></li>`;
            });
        } else {
            content += `
                <details>
                    <summary>${groupName}</summary>
                    <ul>
                        ${modules.map(module => `
                            <li><a href="#module-${module.module.replace(/:/g, '-')}">
                                <span>${module.module}</span>
                                ${showFatalCount ? `<span class="badge badge-fatal">${module.fatal.length}</span>` : ''}
                            </a></li>
                        `).join('')}
                    </ul>
                </details>
            `;
        }
    });
    content += '</ul>';
    return content;
}

function renderSidebarForModules(report) {
    const sidebar = document.getElementById('sidebar');
    const modulesWithFatal = report.modules.filter(m => m.fatal.length > 0).sort((a, b) => a.module.localeCompare(b.module));
    const modulesWithSuppressedOnly = report.modules.filter(m => m.fatal.length === 0 && m.suppressed.length > 0).sort((a, b) => a.module.localeCompare(b.module));

    let content = '<div class="sidebar-header">Modules</div><nav class="sidebar-nav">';

    if (modulesWithFatal.length > 0) {
        content += '<div class="sidebar-section-title">Fatal</div>';
        content += renderModuleGroups(groupModules(modulesWithFatal), true);
    }

    if (modulesWithSuppressedOnly.length > 0) {
        content += '<div class="sidebar-section-title">Suppressed</div>';
        content += renderModuleGroups(groupModules(modulesWithSuppressedOnly), false);
    }

    if (modulesWithFatal.length === 0 && modulesWithSuppressedOnly.length === 0) {
        content += '<p style="color:white; padding: 0 0.5rem;">No matches found.</p>';
    }

    sidebar.innerHTML = content + '</nav>';
}

function groupDependencies(deps) {
    const grouped = {};
    deps.forEach(dep => {
        let groupName = 'ungrouped';
        if (dep.startsWith(':')) { // Module dependency
            const parts = dep.split(':');
            if (parts.length > 2) {
                groupName = parts.slice(0, 2).join(':'); // :domain:a -> :domain
            }
        } else { // Maven dependency
            const parts = dep.split(':');
            if (parts.length > 1) {
                const groupId = parts[0];
                const groupIdParts = groupId.split('.');
                if (groupIdParts.length > 1) {
                    groupName = groupIdParts.slice(0, 2).join('.'); // com.google.android -> com.google
                } else {
                    groupName = groupId;
                }
            }
        }
        if (!grouped[groupName]) {
            grouped[groupName] = [];
        }
        grouped[groupName].push(dep);
    });
    return grouped;
}

function renderDependencyGroups(groupedDependencies, allDependencies, showFatalCount) {
    let content = '<ul>';
    Object.keys(groupedDependencies).sort().forEach(groupName => {
        const deps = groupedDependencies[groupName];
        if (groupName === 'ungrouped') {
            deps.forEach(dep => {
                const fatalCount = showFatalCount ? allDependencies[dep].filter(m => !m.isSuppressed).length : 0;
                content += `<li><a href="#dep-${dep.replace(/[.:]/g, '-')}">
                    <span>${dep}</span>
                    ${showFatalCount ? `<span class="badge badge-fatal">${fatalCount}</span>` : ''}
                </a></li>`;
            });
        } else {
            content += `
                <details>
                    <summary>${groupName}</summary>
                    <ul>
                        ${deps.map(dep => {
                            const fatalCount = showFatalCount ? allDependencies[dep].filter(m => !m.isSuppressed).length : 0;
                            return `<li><a href="#dep-${dep.replace(/[.:]/g, '-')}">
                                <span>${dep}</span>
                                ${showFatalCount ? `<span class="badge badge-fatal">${fatalCount}</span>` : ''}
                            </a></li>`;
                        }).join('')}
                    </ul>
                </details>
            `;
        }
    });
    content += '</ul>';
    return content;
}

function renderSidebarForDependencies(report) {
    const sidebar = document.getElementById('sidebar');
    const dependencies = getMatchesByDependency(report);

    const depsWithFatal = Object.keys(dependencies).filter(dep => dependencies[dep].some(m => !m.isSuppressed)).sort();
    const depsWithSuppressedOnly = Object.keys(dependencies).filter(dep => dependencies[dep].every(m => m.isSuppressed) && !depsWithFatal.includes(dep)).sort();

    let content = '<div class="sidebar-header">Dependencies</div><nav class="sidebar-nav">';

    if (depsWithFatal.length > 0) {
        content += '<div class="sidebar-section-title">Fatal</div>';
        const groupedFatalDeps = groupDependencies(depsWithFatal);
        content += renderDependencyGroups(groupedFatalDeps, dependencies, true);
    }

    if (depsWithSuppressedOnly.length > 0) {
        content += '<div class="sidebar-section-title">Suppressed</div>';
        const groupedSuppressedDeps = groupDependencies(depsWithSuppressedOnly);
        content += renderDependencyGroups(groupedSuppressedDeps, dependencies, false);
    }

     if (depsWithFatal.length === 0 && depsWithSuppressedOnly.length === 0) {
        content = '<p style="color:white; padding: 0 0.5rem;">No matches found.</p>';
    }

    sidebar.innerHTML = content + '</nav>';
}


// --- Tab Content Rendering ---

function renderByModuleView(report) {
    const container = document.getElementById('by-module');
    const sortedModules = report.modules
        .filter(m => m.fatal.length > 0 || m.suppressed.length > 0)
        .sort((a, b) => {
            if (a.fatal.length > 0 && b.fatal.length === 0) return -1;
            if (a.fatal.length === 0 && b.fatal.length > 0) return 1;
            return a.module.localeCompare(b.module);
        });

    if (sortedModules.length === 0) {
        container.innerHTML = '<p>No dependency matches found for any module.</p>';
        return;
    }

    container.innerHTML = sortedModules.map(module => {
        const fatalMatches = module.fatal.map(match => ({ item: match.dependency, reason: match.reason }));
        const suppressedMatches = module.suppressed.map(match => ({ item: match.dependency, reason: match.suppressionReason }));
        const icon = fatalMatches.length > 0 ? 'cancel' : 'block';
        return createRestrictionCard(
            icon,
            module.module,
            `module-${module.module.replace(/:/g, '-')}`,
            `${fatalMatches.length + suppressedMatches.length} dependency restrictions found`,
            fatalMatches,
            suppressedMatches,
            'Matches'
        );
    }).join('');
}

function renderByDependencyView(report) {
    const container = document.getElementById('by-dependency');
    const dependencies = getMatchesByDependency(report);
    const sortedDependencies = Object.keys(dependencies).sort((a, b) => {
        const aHasFatal = dependencies[a].some(m => !m.isSuppressed);
        const bHasFatal = dependencies[b].some(m => !m.isSuppressed);
        if (aHasFatal && !bHasFatal) return -1;
        if (!aHasFatal && bHasFatal) return 1;
        return a.localeCompare(b);
    });

    if (sortedDependencies.length === 0) {
        container.innerHTML = '<p>No dependency matches found.</p>';
        return;
    }

    container.innerHTML = sortedDependencies.map(dep => {
        const matches = dependencies[dep];
        const fatalMatches = matches.filter(v => !v.isSuppressed).map(match => ({ item: match.module, reason: match.reason }));
        const suppressedMatches = matches.filter(v => v.isSuppressed).map(match => ({ item: match.module, reason: match.reason }));
        const icon = fatalMatches.length > 0 ? 'cancel' : 'block';
        return createRestrictionCard(
            icon,
            dep,
            `dep-${dep.replace(/[.:]/g, '-')}`,
            `Consumed by ${matches.length} module(s)`,
            fatalMatches,
            suppressedMatches,
            'Consumers'
        );
    }).join('');
}


// --- HTML Generation Helpers ---

function createRestrictionCard(icon, title, id, subtitle, fatal, suppressed, matchType) {
    const fatalCount = fatal.length;
    const suppressedCount = suppressed.length;
    const hasFatal = fatalCount > 0;

    return `
    <div class="restriction-card" id="${id}">
        <div class="card-header ${hasFatal ? 'has-fatal' : ''}">
            <div class="card-title">
                <div class="card-icon-container">
                    <span class="material-icons">${icon}</span>
                </div>
                <div class="card-title-text">
                    <h3>${title}</h3>
                    <p>${subtitle}</p>
                </div>
            </div>
            <div class="card-actions">
                <div class="card-badges">
                    ${fatalCount > 0 ? `<div class="badge fatal">${fatalCount} Fatal</div>` : ''}
                    ${suppressedCount > 0 ? `<div class="badge suppressed">${suppressedCount} Suppressed</div>` : ''}
                </div>
                <span class="material-icons card-toggle-icon">expand_more</span>
            </div>
        </div>
        <div class="card-body">
            ${fatalCount > 0 ? createMatchesTable(fatal, true, true, matchType) : ''}
            ${suppressedCount > 0 ? createMatchesTable(suppressed, false, fatalCount === 0, matchType) : ''}
        </div>
    </div>
    `;
}

function createMatchesTable(matches, isFatal, isFirstInSection, matchType) {
    const dependencyClass = isFatal ? 'fatal-dependency' : 'suppressed-dependency';
    const title = isFatal ? `Fatal ${matchType}` : `Suppressed ${matchType}`;
    const header = isFatal ? 'Reason for restriction' : 'Reason for suppression';
    const sectionClass = isFirstInSection ? '' : ' extra-margin-top';

    return `
        <div class="matches-section${sectionClass}">
            <h4 class="matches-header">${title}</h4>
            <table class="matches-table">
                <thead>
                    <tr>
                        <th>Dependency</th>
                        <th>${header}</th>
                    </tr>
                </thead>
                <tbody>
                    ${matches.map(match => `
                        <tr>
                            <td><strong class="${dependencyClass}">${match.item}</strong></td>
                            <td>${match.reason}</td>
                        </tr>
                    `).join('')}
                </tbody>
            </table>
        </div>
    `;
}

// --- Data Processing Helpers ---

function getMatchesByDependency(report) {
    const dependencies = {};
    report.modules.forEach(module => {
        module.fatal.forEach(match => {
            const dep = match.dependency;
            if (!dependencies[dep]) dependencies[dep] = [];
            dependencies[dep].push({ module: module.module, reason: match.reason, isSuppressed: false });
        });
        module.suppressed.forEach(match => {
            const dep = match.dependency;
            if (!dependencies[dep]) dependencies[dep] = [];
            dependencies[dep].push({ module: module.module, reason: match.suppressionReason, isSuppressed: true });
        });
    });
    return dependencies;
}
