# 1.13.1 / 2024-04-11

* [BUGFIX] Avoid eager fetching of Variant values. See [#240](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/240)

# 1.13.0 / 2024-04-09

* [FEATURE] Inject build ID into mapping file upload and SDK host application as well. See [#211](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/211)
* [BUGFIX] Add files under kotlin source set to the repository information. See [#212](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/212)
* [MAINTENANCE] Remove use of deprecated env variables. See [#188](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/188)
* [MAINTENANCE] Next dev version. See [#201](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/201)
* [MAINTENANCE] Merge `release/1.12.0` branch into `develop` branch. See [#202](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/202)
* [MAINTENANCE] Use version 1.12.0 of plugin for sample apps. See [#204](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/204)
* [MAINTENANCE] Update Datadog SDK to version 2.3.0. See [#207](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/207)
* [MAINTENANCE] Update build tooling to target API 34. See [#208](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/208)
* [MAINTENANCE] Fix CI visibility integration. See [#209](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/209)
* [MAINTENANCE] Run functional tests against different AGP versions. See [#214](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/214)
* [MAINTENANCE] Update `CODEOWNERS` to include `rum-mobile-android`. See [#213](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/213)
* [MAINTENANCE] Update Datadog SDK to version 2.4.0. See [#215](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/215)
* [MAINTENANCE] Use snake case for properties in mapping file event. See [#217](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/217)
* [MAINTENANCE] Update Datadog SDK to version 2.5.0. See [#218](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/218)
* [MAINTENANCE] Update Datadog SDK to version 2.5.1. See [#219](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/219)
* [MAINTENANCE] Update CMAKE to 3.22.1 for gitlab builds.. See [#221](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/221)
* [MAINTENANCE] Update Datadog SDK to version 2.6.0. See [#223](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/223)
* [MAINTENANCE] Update Datadog SDK to version 2.6.1. See [#224](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/224)
* [MAINTENANCE] Update Datadog SDK to version 2.6.2. See [#225](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/225)
* [MAINTENANCE] Remove non-ASCII characters from test names. See [#226](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/226)
* [MAINTENANCE] Tooling update. See [#227](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/227)
* [MAINTENANCE] Update AGP to 8.3.1. See [#229](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/229)
* [MAINTENANCE] Update Datadog SDK to version 2.7.0. See [#230](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/230)
* [MAINTENANCE] Update Datadog SDK to version 2.7.1. See [#231](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/231)

# 1.12.0 / 2023-10-30

* [IMPROVEMENT] Make plugin compatible with Kotlin Script. See [#197](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/197)
* [IMPROVEMENT] Lower log level from warning to info when no minification enabled. See [#198](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/198)
* [IMPROVEMENT] Allow customer to use non-standard obfuscation. See [#199](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/199)

# 1.11.0 / 2023-09-13

* [BUGFIX] Delete old shrinked mapping file before writing to it. See [#189](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/189)
* [IMPROVEMENT] Read `DATADOG_API_KEY` from gradle properties. See [#186](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/186)
* [MAINTENANCE] Update AGP to 8.1.0. See [#182](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/182)

# 1.10.0 / 2023-07-17

* [IMPROVEMENT] Improve output of gradle plugin upload. See [#173](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/173)
* [IMPROVEMENT] Support dependencies check for both v1 and v2 SDKs. See [#174](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/174)

# 1.9.0 / 2023-05-10

* [IMPROVEMENT] Remove mapping file size check. See [#166](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/166)
* [MAINTENANCE] Support building with Java 17. See [#162](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/162)
* [MAINTENANCE] Bump AGP to version 8.0.1. See [#163](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/163)
* [MAINTENANCE] Setup CodeQL scan. See [#164](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/164)
* [DOCS] Unsingle Source Android Error Tracking Doc Page. See [#165](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/165)

# 1.8.1 / 2023-04-27

* [BUGFIX] Update OkHttp version to 4.11.0. See [#161](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/161)

# 1.8.0 / 2023-03-22

* [FEATURE] Add support to AP1. See [#150](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/150)
* [FEATURE] Add support for Staging upload. See [#151](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/151)
* [IMPROVEMENT] Minor improvements. See [#155](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/155)
* [MAINTENANCE] Next dev version 1.8.0. See [#145](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/145)
* [MAINTENANCE] Update Gradle to version 8.0, update Kotlin, AGP, OkHttp versions as well. See [#148](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/148)
* [MAINTENANCE] Update to version 1.17.1. See [#149](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/149)
* [MAINTENANCE] Fix CI warnings and issues. See [#152](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/152)
* [MAINTENANCE] Update to version 1.17.2. See [#153](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/153)
* [MAINTENANCE] Update to version 1.18.0. See [#156](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/156)
* [DOCS] Improve the documentation for 50Mb limit. See [#141](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/141)

# 1.7.0 / 2023-02-08

- [FEATURE] Add Gzip support for mapping file upload. See [#143](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/143)
- [IMPROVEMENT] Raise upload limit to 100MB for US1 datacenter. See [#142](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/142)

# 1.6.0 / 2023-01-17

- [IMPROVEMENT] Improve mapping upload flow. See [#131](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/131)
- [MAINTENANCE] Dependencies update. See [#133](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/133)
- [CI] Use shared CI template. See [#119](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/119)
- [CI] Update `ktlint` to 0.47.1. See [#121](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/121)

# 1.5.1 / 2022-08-30

- [BUGFIX] Fix a NPE during `datadog-ci.json` file lookup [#112](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/112)

# 1.5.0 / 2022-08-25

- [IMPROVEMENT] Switch to API v2 of the mapping file intake [#99](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/99)
- [IMPROVEMENT] Disable dependencies check task by default [#102](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/102)
- [IMPROVEMENT] Gradle 7.5 & Kotlin 1.6.10 [#103](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/103)
- [IMPROVEMENT] Read config properties from datadog-ci.json [#104](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/104)
- [IMPROVEMENT] Add support of `DATADOG_SITE` and `DATADOG_API_KEY` environment variables [#109](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/109)

# 1.4.0 / 2022-05-17

- [IMPROVEMENT] Add shrinking options for the R8/Proguard/DexGuard mapping file (short aliases, indents trim) [#86](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/86)
- [IMPROVEMENT] Documentation updates [#79](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/79), [#84](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/84)

# 1.3.0 / 2022-03-28

- [IMPROVEMENT] Use OkHttp 4.9.x in plugin dependency
- [IMPROVEMENT] Allow providing a custom path for the R8/Proguard/DexGuard mapping file (e.g.: `datadog { mappingFilePath = "path/to/mapping.txt" }`) [#71](https://github.com/DataDog/dd-sdk-android-gradle-plugin/issues/71) (Thanks [@pietrini](https://github.com/pietrini))

# 1.2.1 / 2021-12-10

- [BUGFIX] Fix dependency check task for project with multiple modules and different flavors

# 1.2.0 / 2021-12-06

- [IMPROVEMENT] Publish plugin on Gradle Portal
- [IMPROVEMENT] Add support to all Datadog regions
- [BUGFIX] Fix Git files path computation

# 1.1.0 / 2021-07-21

- [IMPROVEMENT] Prevent listing git ignored files
- [IMPROVEMENT] Allow customizing the remote repository url
- [IMPROVEMENT] Remove credentials (if any) from remote repository url
- [IMPROVEMENT] Support Gradle configuration cache
- [BUGFIX] Remove Gradle API from dependencies

# 1.0.1 / 2021-04-21

- [BUGFIX] Fix POM file missing dependencies

# 1.0.0 / 2021-03-22

- [FEATURE] Allow uploading Proguard/R8 mapping files for all variants with obfuscation enabled
