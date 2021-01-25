# Gradle Plugin for Datadog Android SDK

> This plugin is used to upload your Proguard/R8 mapping files to Datadog to get a complete Error Tracking experience

## Getting Started

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



## Troubleshooting

If you encounter any issue when using the Datadog SDK for Android, please take a look at 
the existing [issues](https://github.com/DataDog/dd-sdk-android/issues?q=is%3Aissue).

## Contributing

Pull requests are welcome. First, open an issue to discuss what you would like to change. For more information, read the [Contributing Guide](CONTRIBUTING.md).

## License

[Apache License, v2.0](LICENSE)
