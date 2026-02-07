package com.rubensousa.dependencyguard.plugin


class DenyScope internal constructor() {

    internal var denyReason: String = "Unspecified"
    internal val exclusions = mutableSetOf<String>()

    fun setReason(reason: String) {
        denyReason = reason
    }

    fun except(vararg children: String) {
        exclusions.addAll(children)
    }

}
