# Gradle Plugin for Datadog Android SDK

> This plugin is used to upload your Proguard/Dexguard/R8 mapping files and NDK symbol files to Datadog to get a complete RUM Error Tracking experience.

Once your mapping file are uploaded, any error/crash reported through RUM will have a deobfuscated/symbolicated stacktrace, enabling an easy diagnosis of the root cause of the error.

## Getting Started

### Setup

Add the following line to your `build.gradle` file.

```groovy
plugins {
    id("com.datadoghq.dd-sdk-android-gradle-plugin") version "x.y.z"
}
```

### Uploading

To upload your mapping files to Datadog, run the `uploadMapping[Variant]` task in your Android application project as part of your build or after your build, for example:

```bash
./gradlew uploadMappingRelease
```

Similarly, to upload NDK symbols, run the `uploadNdkSymbolFiles[Variant]` task in your Android application project. For example:

```bash
./gradlew uploadNdkSymbolFilesRelease
```

### Configuration

You can configure the plugin by adding the following block at the end of your `build.gradle` file.

```groovy
datadog {
    versionName = "1.3.0" // Optional, by default it is read from your Android plugin configuration's version name
    serviceName = "my-service" // Optional, by default it is read from your Android plugin configuration's package name
    site = "US1" // Optional, can be "US1", "EU1" or "US1_FED", etc. (check `DatadogSite` documentation for the full list). Default is "US1"
    checkProjectDependencies = "warn" // Optional, can be "warn", "fail" or "none". Default is "fail". Will check if Datadog SDK is in the project dependencies.
    mappingFilePath = "path/to/mapping.txt" // Optional, provides a custom mapping file path. Default is "build/outputs/mapping/{variant}/mapping.txt".
    nonDefaultObfuscation = false // Optional, to be used if a 3rd-party obfuscation tool is used. Default is false.
    ignoreDatadogCiFileConfig = false // Optional, ignore configuration provided in `datadog-ci.json` file if found. Default is false.
    additionalSymbolFilesLocations = ["/path/to/location/obj"] // Optional, additional locations the Gradle plugin will check for `.so` files during `uploadNdkSymbolFiles` task. Default is none.
}
```

If you're using variants, you can set a custom configuration per variant using the following syntax.

#### Groovy 

```groovy
datadog {
    site = "US1" // Variants with no configurations will use this as default
    variants {
        fr {
            site = "EU1"
            mappingFilePath = "path/to/fr/mapping.txt"
        }
    }
}
```

#### Kotlin Script

```kotlin
datadog {
    site = "US1" // Variants with no configurations will use this as default
    variants {
        register("fr") {
            site = "EU1"
            mappingFilePath = "path/to/fr/mapping.txt"
        }
    }
}
```

### Gradle configuration cache support

This plugin supports [Gradle configuration cache](https://docs.gradle.org/7.1/userguide/configuration_cache.html) starting from the version `1.1.0`, but to have this support you need to disable SDK dependency check by setting `checkProjectDependencies` to `none`:

```
datadog {
    ...
    checkProjectDependencies = "none"
    ...
}
```

For more information, see [Android Crash Reporting and Error Tracking](https://docs.datadoghq.com/real_user_monitoring/error_tracking/android/).

## Troubleshooting

If you encounter any issue when using the Gradle Plugin for Datadog Android SDK, please take a look at 
the existing [issues](https://github.com/DataDog/dd-sdk-android/issues?q=is%3Aissue).

## Contributing

Pull requests are welcome. First, open an issue to discuss what you would like to change. For more information, read the [Contributing Guide](CONTRIBUTING.md).

## License

[Apache License, v2.0](LICENSE)
