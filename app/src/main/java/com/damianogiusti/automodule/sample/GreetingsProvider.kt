package com.damianogiusti.automodule.sample

import com.damianogiusti.automodule.annotations.AutoModule
import dagger.Reusable

/**
 * Created by Damiano Giusti on 09/04/2021.
 */
interface GreetingsProvider {
    fun greet(): String
}

@Reusable
@AutoModule(SampleModule::class)
class DefaultGreetingsProvider : GreetingsProvider {

    override fun greet(): String = "Hello, world!"
}