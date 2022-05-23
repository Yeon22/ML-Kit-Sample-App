package com.bsuyeon.mlkitinterlock

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry


class CustomLifeCycle: LifecycleOwner {
    private var mLifecycleRegistry: LifecycleRegistry = LifecycleRegistry(this)

    init {
        mLifecycleRegistry.markState(Lifecycle.State.CREATED)
    }

    fun doOnResume() {
        mLifecycleRegistry.markState(Lifecycle.State.RESUMED)
    }

    fun doOnStart() {
        mLifecycleRegistry.markState(Lifecycle.State.STARTED)
    }

    override fun getLifecycle(): Lifecycle {
        return mLifecycleRegistry
    }
}