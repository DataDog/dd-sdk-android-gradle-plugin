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
