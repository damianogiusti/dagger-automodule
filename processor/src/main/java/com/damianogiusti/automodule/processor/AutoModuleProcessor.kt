package com.damianogiusti.automodule.processor

import com.damianogiusti.automodule.annotations.AutoModule
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
import dagger.Binds
import dagger.Module
import dagger.hilt.migration.DisableInstallInCheck
import net.ltgt.gradle.incap.IncrementalAnnotationProcessor
import net.ltgt.gradle.incap.IncrementalAnnotationProcessorType
import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Filer
import javax.annotation.processing.Messager
import javax.annotation.processing.ProcessingEnvironment
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.MirroredTypeException
import javax.lang.model.type.TypeKind
import javax.tools.Diagnostic
import javax.tools.StandardLocation
import kotlin.reflect.KClass

/**
 * Custom annotation processor for generating binding modules for Dagger2.
 *
 * Given an implementation `MyClassImpl` bound to an interface `MyClass` using
 * the [AutoModule] annotation into a `MyBindingModule`, it will generate an interface named
 * `AutoModuleMyBindingModule` that will contain the bindings for `MyClassImpl` to `MyClass`.
 *
 * Usage:
 *
 * ```
 * interface MyClass {
 * }
 *
 * @AutoModule(module = MyBindingModule::class)
 * class MyClassImpl @Inject constructor() : MyClass
 *
 * @Module
 * interface MyBindingModule : /* generated -> */  AutoModuleMyBindingModule
 * ```
 *
 * Created by Damiano Giusti on 28/09/2020.
 */
@AutoService(Processor::class)
@IncrementalAnnotationProcessor(IncrementalAnnotationProcessorType.AGGREGATING)
class AutoModuleProcessor : AbstractProcessor() {

    data class GeneratedModule(
        val moduleName: String,
        val packageName: String,
        val typeSpec: TypeSpec
    )

    private val platform: Platform = Platform.get()
    private var filer: Filer? = null
    private var messager: Messager? = null

    override fun init(processingEnv: ProcessingEnvironment?) {
        super.init(processingEnv)
        messager = processingEnv?.messager
        filer = processingEnv?.filer
    }

    override fun process(
        annotations: MutableSet<out TypeElement>,
        roundEnv: RoundEnvironment
    ): Boolean {
        val filer = filer ?: return false
        val annotatedClasses = roundEnv.getAnnotatedClasses()
        if (annotatedClasses.isEmpty()) return false

        val classesByModule = annotatedClasses
            .groupBy { cls -> cls.annotation().getTypeElement { module } }

        val daggerModuleTypeSpecs = classesByModule.map { (moduleClass, annotatedClasses) ->
            val moduleBindingMethods = annotatedClasses.mapNotNull { annotatedClass -> createBindsMethodOrNull(annotatedClass) }
            createModuleTypeSpec(moduleClass, moduleBindingMethods)
        }

        daggerModuleTypeSpecs.forEach { generatedModule ->
            writeModuleIntoFile(filer, generatedModule)
        }

        return true
    }

    private fun createBindsMethodOrNull(annotatedClass: TypeElement): FunSpec? {
        val parentInterface = annotatedClass.interfaces.singleOrNull()
        return if (parentInterface == null) {
            messager?.printMessage(Diagnostic.Kind.ERROR, "Must have only one parent interface", annotatedClass)
            null
        } else {
            createBindsFunctionSpec(annotatedClass.asClassName(), parentInterface.asTypeName())
        }
    }

    private fun writeModuleIntoFile(filer: Filer, generatedModule: GeneratedModule) {
        val (moduleName, modulePackageName, moduleTypeSpec) = generatedModule
        val outputFile = filer.createOutFile(modulePackageName, moduleName)
        outputFile.openWriter().use { writer ->
            FileSpec.builder(modulePackageName, moduleName)
                .addType(moduleTypeSpec)
                .build()
                .writeTo(writer)
        }
    }

    private fun createModuleTypeSpec(moduleClass: TypeElement, functions: List<FunSpec>): GeneratedModule {
        val moduleClassName = moduleClass.asClassName()
        val (outPackageName, moduleClassSimpleName) = moduleClassName.toPackageAndName()
        val moduleName = "AutoModule$moduleClassSimpleName"
        val typeSpec = TypeSpec
            .interfaceBuilder(moduleName)
            .addAnnotation(Module::class.java)
            .apply {
                if (platform.isHiltPresent()) {
                    addAnnotation(DisableInstallInCheck::class.java)
                }
            }
            .addFunctions(functions)

        return GeneratedModule(moduleName, outPackageName, typeSpec.build())
    }

    private fun ClassName.toPackageAndName() = canonicalName.split(".").run {
        dropLast(1).joinToString(separator = ".") to last()
    }

    private fun createBindsFunctionSpec(concreteClass: ClassName, abstractClass: TypeName) = FunSpec
        .builder("binds${concreteClass.simpleName}")
        .addModifiers(KModifier.ABSTRACT)
        .addAnnotation(Binds::class.java)
        .addParameter("impl", concreteClass)
        .returns(abstractClass)
        .build()

    private fun RoundEnvironment.getAnnotatedClasses(): List<TypeElement> =
        getElementsAnnotatedWith(AutoModule::class.java).mapNotNull { it as? TypeElement }

    private fun TypeElement.annotation(): AutoModule = getAnnotation(AutoModule::class.java)

    private fun <T> T.getTypeElement(block: T.() -> KClass<*>): TypeElement = try {
        // Workaround for getting a TypeElement from a KClass. Accessing a KClass in an
        // annotation processor will result in a MirroredTypeException. But into this exception
        // we will be able to find the TypeElement we were looking for. Tricky.
        block().let { null }
    } catch (e: MirroredTypeException) {
        if (e.typeMirror.kind == TypeKind.DECLARED) { // TypeKind.DECLARED: classes and interfaces
            ((e.typeMirror as DeclaredType).asElement() as TypeElement)
        } else null
    } ?: error("Unable to get TypeMirror for ${javaClass.simpleName}")

    private fun Filer.createOutFile(packageName: String, name: String) =
        createResource(StandardLocation.SOURCE_OUTPUT, packageName, "$name.kt")

    override fun getSupportedSourceVersion(): SourceVersion {
        return SourceVersion.latestSupported()
    }

    override fun getSupportedAnnotationTypes(): MutableSet<String> {
        return mutableSetOf(AutoModule::class.java.name)
    }
}
