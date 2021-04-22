## Overview

Error Tracking processes errors collected from the Android SDK. To quickly get started with error tracking download the latest version of [dd-sdk-android][1].

If your mobile Android source code is obfuscated, upload your Proguard/R8 mapping file to Datadog so that your different stack traces can be deobfuscated. For a given error, you can get access to the file path, the line number, as well as a code snippet for each frame of the related stack trace.

## Upload your mapping file

{{< tabs >}}
{{% tab "US" %}}

1. Add the [Gradle Plugin][1] to your Gradle project, using the following snippet.

```groovy
// In your project's build.gradle script
buildscript {
    dependencies {
        classpath("com.datadoghq:dd-sdk-android-gradle-plugin:x.x.x")
    }
}

// In your app's build.gradle script
plugins {
    id("dd-sdk-android-gradle-plugin")
}
```

2. [Create a new and dedicated Datadog API key][2] and export it as an environment variable named `DD_API_KEY`.
3. (Optional) Configure the plugin to upload files to the EU region by exporting two additonal environment variables: `export DATADOG_SITE="datadoghq.eu"` and `export DATADOG_API_HOST="api.datadoghq.eu"`.
4. Run the upload task after your obfuscated APK has been built:
```bash
./gradlew uploadMappingRelease
```
   **Note**: If your project uses additional flavors, the plugin will provide an upload task for each variant with obfuscation enabled.

[1]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
[2]: https://app.datadoghq.com/account/settings#api

{{% /tab %}}
{{% tab "EU" %}}
1. Add the [Gradle Plugin][1] to your Gradle project, using the following snippet.

```groovy
// In your project's build.gradle script
buildscript {
    dependencies {
        classpath("com.datadoghq:dd-sdk-android-gradle-plugin:x.x.x")
    }
}

// In your app's build.gradle script
plugins {
    id("dd-sdk-android-gradle-plugin")
}
```

2. [Create a new and dedicated Datadog API key][2] and export it as an environment variable named `DD_API_KEY`.
3. Configure the plugin to use the EU region by adding the following snippet in your app's `build.gradle` script file:

```groovy
datadog {
    site = "EU"
}
```
4. Run the upload task after your obfuscated APK has been built:
```bash
./gradlew uploadMappingRelease
```
**Note**: If your project uses additional flavors, the plugin will provide an upload task for each variant with obfuscation enabled.

[1]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
[2]: https://app.datadoghq.com/account/settings#api
{{% /tab %}}
{{< /tabs >}}



## Troubleshoot errors

An obfuscated stack trace is not helpful as you don't have access to the class name, file path and the line number. It's hard to know where something is happening in your code base. In addition, the code snippet is still minified (one long line of transformed code) which makes the troubleshooting process even harder. See below an example of an minified stack trace:

![image_obfuscated][2]

On the contrary, a deobfuscated stack trace gives you all the context you need for troubleshooting:

![image_deobfuscated][3]

[1]: https://github.com/DataDog/dd-sdk-android
[2]: https://raw.githubusercontent.com/DataDog/dd-sdk-android-gradle-plugin/main/docs/images/obfuscated_stacktrace.png
[3]: https://raw.githubusercontent.com/DataDog/dd-sdk-android-gradle-plugin/main/docs/images/deobfuscated_stacktrace.png

## Further Reading

{{< partial name="whats-next/whats-next.html" >}}
