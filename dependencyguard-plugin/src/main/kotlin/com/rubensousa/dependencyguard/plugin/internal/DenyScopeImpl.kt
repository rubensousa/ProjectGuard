package com.rubensousa.dependencyguard.plugin.internal

import com.rubensousa.dependencyguard.plugin.DenyScope

internal class DenyScopeImpl : DenyScope {

    internal var denyReason: String = "Unspecified"

    override fun setReason(reason: String) {
        denyReason = reason
    }
}