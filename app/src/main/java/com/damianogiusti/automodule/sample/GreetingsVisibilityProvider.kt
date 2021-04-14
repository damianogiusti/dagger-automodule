package com.damianogiusti.automodule.sample

import com.damianogiusti.automodule.annotations.AutoModule
import com.damianogiusti.automodule.annotations.ModuleVisibility
import dagger.Reusable

/**
 * Created by Federico Monti on 14/04/2021.
 */
internal interface GreetingsVisibilityProvider {
    fun greet(): String
}

@Reusable
@AutoModule(SampleVisibilityModule::class, ModuleVisibility.INTERNAL)
internal class DefaultGreetingsVisibilityProvider : GreetingsVisibilityProvider {

    override fun greet(): String = "Hello, world! From today.. with a whole new visibility!"
}
