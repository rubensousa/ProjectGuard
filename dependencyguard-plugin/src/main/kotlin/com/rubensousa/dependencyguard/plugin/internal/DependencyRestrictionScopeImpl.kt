package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.AllowScope
import com.rubensousa.dependencyguard.plugin.DependencyRestrictionScope
import com.rubensousa.dependencyguard.plugin.SuppressScope
import org.gradle.api.Action

internal class DependencyRestrictionScopeImpl : DependencyRestrictionScope {

    private val allowed = mutableListOf<ModuleSpec>()
    private val suppressed = mutableListOf<ModuleSpec>()
    private var restrictionReason = "Unspecified"

    override fun setReason(reason: String) {
        restrictionReason = reason
    }

    override fun allow(
        modulePath: String,
        action: Action<AllowScope>,
    ) {
        val scope = AllowScopeImpl()
        action.execute(scope)
        allowed.add(
            ModuleSpec(
                modulePath = modulePath,
                reason = scope.getReason()
            )
        )
    }

    override fun suppress(modulePath: String, action: Action<SuppressScope>) {
        val scope = SuppressScopeImpl()
        action.execute(scope)
        suppressed.add(
            ModuleSpec(
                modulePath = modulePath,
                reason = scope.getReason()
            )
        )
    }

    fun getAllowedModules() = allowed.toList()

    fun getSuppressedModules() = suppressed.toList()

    fun getReason() = restrictionReason


}
