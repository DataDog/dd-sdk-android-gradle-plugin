## Overview

Error Tracking processes errors collected from the RUM Android SDK. 

Enable Android Crash Reporting and Error Tracking to get comprehensive crash reports and error trends with Real User Monitoring. With this feature, you can access:

- Aggregated Android crash dashboards and attributes
- Deobfuscated Android crash reports
- Trend analysis with Android error tracking

Your crash reports appear in [**Error Tracking**][1].

## Setup

If you have not set up the Android SDK yet, follow the [in-app setup instructions][2] or see the [Android RUM setup documentation][3].

1. Download the latest version of the [RUM Android SDK][4].
2. Configure your application's `version`, `env`, and `service` when [initializing the SDK][5].
3. Upload your Proguard/R8 mapping file to Datadog to access deobfuscated stack traces. 

For any given error, you can access to the file path, line number, as well as a code snippet for each frame of the related stack trace.

## Upload your mapping file

{{< tabs >}}
{{% tab "US" %}}

1. Add the [Android Gradle Plugin][1] to your Gradle project using the following code snippet.

   ```groovy
   // In your app's build.gradle script
   plugins {
       id("com.datadoghq.dd-sdk-android-gradle-plugin") version "x.y.z"
   }
   ```

2. [Create a dedicated Datadog API key][2] and export it as an environment variable named `DD_API_KEY` (alternatively, pass it as a task property).
3. Optionally, configure the plugin to upload files to the EU region by exporting two additional environment variables: `export DATADOG_SITE="datadoghq.eu"` and `export DATADOG_API_HOST="api.datadoghq.eu"`.
4. Run the upload task after your obfuscated APK builds:
    
   ```bash
   ./gradlew uploadMappingRelease
   ```

**Note**: If your project uses additional flavors, the plugin provides an upload task for each variant with obfuscation enabled. In this case, initialize the RUM Android SDK with a proper variant name (the necessary API is available in versions `1.8.0` and later).

[1]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
[2]: https://app.datadoghq.com/account/settings#api

{{% /tab %}}
{{% tab "EU" %}}
1. Add the [Android Gradle Plugin][1] to your Gradle project using the following code snippet.

   ```groovy
   // In your app's build.gradle script
   plugins {
       id("com.datadoghq.dd-sdk-android-gradle-plugin") version "x.y.z"
   }
   ```

2. [Create a dedicated Datadog API key][2] and export it as an environment variable named `DD_API_KEY` (alternatively, pass it as a task property).
3. Configure the plugin to use the EU region by adding the following snippet in your app's `build.gradle` script file:

   ```groovy
   datadog {
       site = "EU"
   }
   ```

4. Run the upload task after your obfuscated APK builds:
   
   ```bash
   ./gradlew uploadMappingRelease
   ```

**Note**: If your project uses additional flavors, the plugin provides an upload task for each variant with obfuscation enabled. In this case, initialize the RUM Android SDK with a proper variant name (the necessary API is available in versions `1.8.0` and later).

[1]: https://github.com/DataDog/dd-sdk-android-gradle-plugin
[2]: https://app.datadoghq.com/account/settings#api

{{% /tab %}}
{{< /tabs >}}

### Plugin Configuration Options

There are several plugin properties that can be configured through the plugin extension. In case you are using multiple variants, you can set a property value for a specific flavor in the variant.

For example, for a `fooBarRelease` variant, you can use the following configuration:

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

The task configuration for this variant is merged from all three flavor configurations provided in the following order:

1. `bar`
2. `foo`
3. `fooBar`

This resolves the final value for the `versionName` property as `fooBar`.

| Property name              | Description                                                                                                                                                                                               |
|----------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `versionName`              | The version name of the application.                                                                                                                                                                      |
| `serviceName`              | The service name of the application.                                                                                                                                                                      |
| `site`                     | The Datadog site to upload your data to (US, US3, US5, EU, or GOV)..                                                                                                                                       |
| `remoteRepositoryUrl`      | The URL of the remote repository where the source code was deployed. If this is not provided, this value is resolved from your Git configuration during the task execution time.                     |
| `checkProjectDependencies` | This property controls if the plugin should check if the Datadog Android SDK is included in the dependencies. If not, "none" is ignored, "warn" logs a warning, and "fail" fails the build with an error (default). |

### Integrate with a CI/CD pipeline

By default, the upload mapping task is independent from other tasks in the build graph. Run the task manually when you need to upload mapping.

If you want to run this task in a CI/CD pipeline, and the task is required as part of the build graph, you can set the upload task to run after the mapping file is generated.

For example:

```groovy
tasks["minify${variant}WithR8"].finalizedBy { tasks["uploadMapping${variant}"] }
```

## Further Reading

{{< partial name="whats-next/whats-next.html" >}}

[1]: https://app.datadoghq.com/rum/error-tracking
[2]: https://app.datadoghq.com/rum/application/create
[3]: https://docs.datadoghq.com/real_user_monitoring/android/#setup
[4]: https://github.com/DataDog/dd-sdk-android
[5]: https://docs.datadoghq.com/real_user_monitoring/android/advanced_configuration/?tabs=kotlin#initialization-parameters
