package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.AllowScope

internal class AllowScopeImpl : AllowScope {

    private var allowReason: String = "Unspecified"

    override fun setReason(reason: String) {
        allowReason = reason
    }

    fun getReason(): String = allowReason

}