## Overview

Error Tracking processes errors collected from the Android SDK. To quickly get started with error tracking download the latest version of [dd-sdk-android][1].

If your mobile Android source code is obfuscated, upload your Proguard/R8 mapping file to Datadog so that your different stack traces can be deobfuscated. For a given error, you can get access to the file path, the line number, as well as a code snippet for each frame of the related stack trace.

## Upload your mapping file

{{< tabs >}}
{{% tab "US" %}}

1. Add the [Gradle Plugin][1] to your Gradle project, using the following snippet.

```groovy
// In your app's build.gradle script
plugins {
    id("com.datadoghq.dd-sdk-android-gradle-plugin") version "x.y.z"
}
```

2. [Create a new and dedicated Datadog API key][2] and export it as an environment variable named `DD_API_KEY` (alternatively you can pass it as a task property).
3. (Optional) Configure the plugin to upload files to the EU region by exporting two additional environment variables: `export DATADOG_SITE="datadoghq.eu"` and `export DATADOG_API_HOST="api.datadoghq.eu"`.
4. Run the upload task after your obfuscated APK has been built:
```bash
./gradlew uploadMappingRelease
```
**Note**: If your project uses additional flavors, the plugin provides an upload task for each variant with obfuscation enabled. In this case, initialize SDK with a proper variant name (the necessary API is available since version `1.8.0`).

[1]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
[2]: https://app.datadoghq.com/account/settings#api

{{% /tab %}}
{{% tab "EU" %}}
1. Add the [Gradle Plugin][1] to your Gradle project, using the following snippet.

```groovy
// In your app's build.gradle script
plugins {
    id("com.datadoghq.dd-sdk-android-gradle-plugin") version "x.y.z"
}
```

2. [Create a new and dedicated Datadog API key][2] and export it as an environment variable named `DD_API_KEY` (alternatively you can pass it as a task property).
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
**Note**: If your project uses additional flavors, the plugin provides an upload task for each variant with obfuscation enabled. In this case, initialize SDK with a proper variant name (the necessary API is available since version `1.8.0`).

[1]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
[2]: https://app.datadoghq.com/account/settings#api
{{% /tab %}}
{{< /tabs >}}

### Plugin Configuration Options

There are several plugin properties that can be configured through the plugin extension. In case you are using multiple variants you can set a property value for a specific flavour in the variant.

**Example:**

For a variant `fooBarRelease` you could have the following configuration:

```groovy
datadog {
    foo {
        versionName = "foo"
    }
    bar {
        versionName = "bar"
    }
    fooBar {
        versionName = "fooBar"
    }
}
```

The task config for this variant will be merged from all the 3 provided flavours configs in the
following order: `bar` -> `foo` -> `fooBar` which will resolve the final value for the `versionName`
property as : `fooBar`

| Property name              | Description                                                                                                                                                                                               |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `versionName`              | The version name of the application.                                                                                                                                                                      |
| `serviceName`              | The service name of the application.                                                                                                                                                                      |
| `site`                     | The Datadog site to upload your data to (one of "US", "EU", "GOV").                                                                                                                                       |
| `remoteRepositoryUrl`      | The url of the remote repository where the source code was deployed.  If not provided this value will be resolved from your current GIT configuration during the task execution time.                     |
| `checkProjectDependencies` | This property controls if plugin should check if Datadog SDK is included in the dependencies and if it is not:  "none" - ignore, "warn" - log a warning, "fail" - fail the build with an error (default). |

### Integration with CI/CD pipeline

By default, the upload mapping task is independent from other tasks in the build graph. Run the task manually when you need to upload mapping.

If you want to run this task in a CI/CD pipeline, and the task is required as part of the build graph, you can set the upload task to run after the mapping file is generated.

For example:

```groovy
tasks["minify${variant}WithR8"].finalizedBy { tasks["uploadMapping${variant}"] }
```

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
