package com.damianogiusti.automodule.processor

/**
 * Created by Damiano Giusti on 09/04/2021.
 */
internal interface Platform {

    fun isHiltPresent(): Boolean

    companion object {
        fun get(): Platform = PlatformImpl()
    }
}

private class PlatformImpl : Platform {

    override fun isHiltPresent(): Boolean {
       return isClassPresent("dagger.hilt.components.SingletonComponent")
    }

    private fun isClassPresent(name: String): Boolean {
        try {
            Class.forName(name)
        } catch (ignored: ClassNotFoundException) {
            return false
        }
        return true
    }
}