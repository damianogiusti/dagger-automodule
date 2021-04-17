package com.damianogiusti.automodule.annotations

import java.lang.reflect.Modifier
import kotlin.reflect.KClass

/**
 * Automagically bounds the annotated class to its parent interface generating a binding
 * that will be written into an interface.
 *
 * Such interface will have the same name of the [module], with "AutoModule" as prefix.
 *
 * This annotation can be used only on classes with a single parent interface.
 *
 * Created by Damiano Giusti on 28/09/2020.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class AutoModule(
    val module: KClass<*>,
    val moduleVisibility: ModuleVisibility = ModuleVisibility.PUBLIC
)