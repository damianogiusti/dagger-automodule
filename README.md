[![Release](https://jitpack.io/v/damianogiusti/dagger-automodule.svg)](https://jitpack.io/#damianogiusti/dagger-automodule)

# Dagger AutoModule

**Dagger AutoModule** is an annotation processor which allows to generate a default implementation
for Dagger modules that are meant to bind interfaces with implementations. 
And yes, Hilt is supported too!

Stop writing bindings boilerplate code! 

## Installation 

This project is distributed using [JitPack](https://jitpack.io/#damianogiusti/dagger-automodule).
Please make sure to add its repository to your project's `build.gradle`.

```groovy
allprojects {
    repositories {
        maven { url 'https://jitpack.io' }
    }
}
```

Then, you need to import AutoModule's annotations and annotation processor packages. 
In your module's `build.gradle` add the following imports:

```groovy
implementation "com.github.damianogiusti.dagger-automodule:automodule-annotations:<version>"
kapt "com.github.damianogiusti.dagger-automodule:automodule-processor:<version>"
```

## Usage

AutoModule generates a Dagger module containing bindings for interfaces with their implementors.

First, you need to create an empty placeholder module, that will be used as a target for bindings.

```kotlin
@Module
interface SampleModule {
}
```

Then, you need to annotate a class that implements an interface with the `@AutoModule` annotation.

```kotlin
interface GreetingsProvider {
    fun greet(): String
}

@AutoModule(SampleModule::class)
class ProgrammerGreetingsProvider @Inject constructor() : Greeter {  
    override fun greet(): String = "Hello, world!"
}
```

This will generate a new Dagger module called `AutoModuleSampleModule` that will contain 
the binding for the class and the parent interface:

```kotlin
// GENERATED

import dagger.Binds
import dagger.Module

@Module
interface AutoModuleSampleModule {
  @Binds
  fun bindsProgrammerGreetingsProvider(impl: ProgrammerGreetingsProvider): GreetingsProvider
}
```

> ⚠️ AutoModule only supports classes with a single parent interface.

Last, you need to use the generated module as **parent interface** of your target module, like so:

```kotlin
@Module
interface SampleModule : AutoModuleSampleModule {
}
```

Add your module to your desired Component, or use the `@InstallIn` annotation provided by **Hilt**.

## Author

Damiano Giusti - [damianogiusti.com](https://damianogiusti.com/)