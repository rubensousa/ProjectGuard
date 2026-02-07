package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.AllowScope
import com.rubensousa.dependencyguard.plugin.SuppressScope

internal class SuppressScopeImpl : SuppressScope {

    private var suppressReason: String = "Unspecified"

    override fun setReason(reason: String) {
        suppressReason = reason
    }

    fun getReason(): String = suppressReason

}
