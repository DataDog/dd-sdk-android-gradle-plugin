# Gradle Plugin for Datadog Android SDK

> This plugin is used to upload your Proguard/R8 mapping files to Datadog to get a complete Error Tracking experience

## Getting Started

### Setup

Add the following line to your `build.gradle` file.

```groovy
buildscript {
    dependencies {
        classpath("com.datadoghq:dd-sdk-android-gradle-plugin:x.x.x")
    }
}

plugins {
    id("dd-sdk-android-gradle-plugin")
}
```

### Configuration

You can configure the plugin by adding the following block at the end of your `build.gradle` file.

```groovy
datadog {
    versionName = "1.3.0" // Optional, by default it is read from your Android plugin configuration's version name
    serviceName = "my-service" // Optional, by default it is read from your Android plugin configuration's package name
    site = "US" // Optional, can be "US", "EU" or "GOV". Default is "US"
    checkProjectDependencies = "warn" // Optional, can be "warn", "fail" or "none". Default is "fail". Will check if Datadog SDK is in the project dependencies.
}
```

If you're using variants, you can set a custom configuration per variant using the following syntax.

```groovy
datadog {
    site = "US" // Variants with no configurations will use this as default
    variants {
        fr {
            site = "EU"
            checkProjectDependencies = "warn"
        }
    }
}
```

## Troubleshooting

If you encounter any issue when using the Gradle Plugin for Datadog Android SDK, please take a look at 
the existing [issues](https://github.com/DataDog/dd-sdk-android/issues?q=is%3Aissue).

## Contributing

Pull requests are welcome. First, open an issue to discuss what you would like to change. For more information, read the [Contributing Guide](CONTRIBUTING.md).

## License

[Apache License, v2.0](LICENSE)
