# 1.22.0 / 2026-01-19

* [IMPROVEMENT] Don't fail NDK upload task if architecture is not supported. See [#465](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/465)
* [IMPROVEMENT] `OkHttpUploader`: Fix response not being closed in `handleResponse()`. See [#469](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/469)
* [IMPROVEMENT] Support AGP 9.0. See [#478](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/478)
* [MAINTENANCE] Update Datadog Gradle Plugin to version 1.21.0. See [#452](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/452)
* [MAINTENANCE] Update files missing copyright. See [#455](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/455)
* [MAINTENANCE] Bump `dd-trace-java` version to 1.54.0. See [#456](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/456)
* [MAINTENANCE] Update Datadog SDK to version 3.2.0. See [#457](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/457)
* [MAINTENANCE] Add `allTests` to include all the unit tests in gradle plugin. See [#459](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/459)
* [MAINTENANCE] Update Datadog SDK to version 3.3.0. See [#468](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/468)
* [MAINTENANCE] Update Datadog SDK to version 3.4.0. See [#473](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/473)
* [MAINTENANCE] Use Kotlin 2.3 and Gradle 9.2.1 for the latest configuration in functional test runs. See [#476](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/476)
* [MAINTENANCE] Update Datadog SDK to version 3.5.0. See [#480](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/480)

# 1.21.0 / 2025-10-02

* [IMPROVEMENT] Unbox default type to fix Kotlin 2.2.20 breaking change. See [#440](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/449)
* [BUGFIX] Fix Kotlin Compiler Plugin wrong jar path for Windows. See [#448](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/448)
* [MAINTENANCE] Next dev iteration. See [#440](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/440)
* [MAINTENANCE] Merge `release/1.20.0` branch into `develop` branch. See [#442](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/442)
* [MAINTENANCE] Update Datadog Gradle Plugin to version 1.20.0. See [#443](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/443)
* [MAINTENANCE] Update Datadog SDK to version 3.1.0. See [#446](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/446)
* [MAINTENANCE] Update Datadog SDK snapshot version used to `3.2.0-SNAPSHOT`. See [#447](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/447)

# 1.20.0 / 2025-09-15

* [BUGFIX] Fix Gradle 9.0 support. See [#435](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/435)
* [MAINTENANCE] Add Github action to automatically close stale issues. See [#427](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/427)
* [MAINTENANCE] Update Datadog Gradle Plugin to version 1.19.0. See [#423](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/423)
* [MAINTENANCE] Update Datadog SDK to version 2.26.0. See [#428](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/428)
* [MAINTENANCE] Bump `slack-notifier` version. See [#430](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/430)
* [MAINTENANCE] Update Datadog SDK to version 3.0.0. See [#431](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/431)
* [MAINTENANCE] Bump `dd-trace-java` version to 1.53.0. See [#433](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/433)
* [MAINTENANCE] Update latest test configuration for the functional tests. See [#434](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/434)
* [DOCS] Fix link to `CONTRIBUTING` from PR template. See [#438](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/438)

# 1.19.0 / 2025-08-19

* [IMPROVEMENT] Configure gradle file to create multiple source sets for Compiler Plugin. See [#412](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/412)
* [IMPROVEMENT] Move Kotlin Compiler logic into `Kotlin20` source sets. See [#413](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/413)
* [IMPROVEMENT] Add Kotlin `2.2.0` support in Kotlin Compiler Plugin. See [#414](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/414)
* [IMPROVEMENT] Add Kotlin `2.1.x` support in Kotlin Compiler Plugin. See [#415](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/415)
* [IMPROVEMENT] Fix `IrFunctionExpressionImpl` in Kotlin `2.0.0 - 2.0.10`. See [#416](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/416)
* [IMPROVEMENT] Add unit test for each Kotlin source set. See [#417](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/417)
* [IMPROVEMENT] Upgrade Kotlin Version to `2.1.21` to remove metadata check error. See [#420](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/420)
* [IMPROVEMENT] Merge `Feature/kotlin-source-sets` into `develop`. See [#421](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/421)
* [MAINTENANCE] Migrate to Central Publisher portal. See [#404](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/404)
* [MAINTENANCE] Use Maven Publish plugin for the publication. See [#406](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/406)
* [MAINTENANCE] Update URL for pulling snapshots. See [#405](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/405)
* [MAINTENANCE] Update Datadog SDK to version `2.24.0`. See [#409](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/409)
* [MAINTENANCE] Update Datadog SDK to version `2.25.0`. See [#410](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/410)
* [MAINTENANCE] Update docker image for `ci-image` job to Jammy. See [#397](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/397)
* [MAINTENANCE] Next dev iteration. See [#399](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/399)
* [MAINTENANCE] Merge `release/1.18.0` branch into `develop` branch. See [#401](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/401)
* [MAINTENANCE] Update Datadog Gradle Plugin to version `1.18.0`. See [#402](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/402)
* [MAINTENANCE] Use `dd-octo-sts` to access GitHub REST API. See [#403](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/403)
* [MAINTENANCE] 🔒 Pin GitHub Actions to specific SHA versions for enhanced security. See [#418](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/418)

# 1.18.0 / 2025-06-30

* [FEATURE] Add `AP2` datacenter support. See [#385](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/385)
* [IMPROVEMENT] Support Material 3 Icon in Kotlin Compiler Plugin. See [#384](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/384)
* [MAINTENANCE] Next dev iteration. See [#379](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/379)
* [MAINTENANCE] Update Datadog Gradle Plugin to version 1.17.0. See [#378](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/378)
* [MAINTENANCE] Allow PR creation by `dd-sdk-android` repository using `octo-sts`. See [#387](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/387)
* [MAINTENANCE] Allow content write for `dd-sdk-android`. See [#388](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/388)
* [MAINTENANCE] Update Datadog SDK version to 2.23.0. See [#391](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/391)
* [MAINTENANCE] Use tag in `octo-sts-subject-pattern`. See [#392](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/392)
* [MAINTENANCE] Update base Docker image to Ubuntu Jammy. See [#394](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/394)
* [MAINTENANCE] Update latest build tooling versions config for the functional test. See [#395](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/395)
* [MAINTENANCE] Add `dd-octo-sts` to Docker image and add policy YAMLs. See [#396](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/396)

# 1.17.0 / 2025-06-02

* [MAINTENANCE] Update Datadog SDK to 2.22.0.
  See [#376](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/376)
* [MAINTENANCE] Use SDK snapshot version for instrumented test.
  See [#372](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/372)

# 1.17.0-rc1 / 2025-04-28

* [FEATURE] Add Kotlin Compiler Plugin environment.
  See [#314](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/314)
* [FEATURE] Add `ComposeTagTransformer` to inject `datadog` modifier call.
  See [#321](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/321)
* [FEATURE] Register plugin to Kotlin compile options.
  See [#322](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/322)
* [FEATURE] Implement annotation mode for instrumentation.
  See [#342](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/342)
* [FEATURE] Implement `NavHost` instrumentation transformer.
  See [#329](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/329)
* [FEATURE] Implement Kotlin Compiler Plugin DSL configuration.
  See [#330](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/330)
* [FEATURE] Add `semantics role` for `Image`, `Icon`, and `AsyncImage`.
  See [#334](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/334)
* [FEATURE] Use composable function name as the semantics tag.
  See [#337](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/337)
* [IMPROVEMENT] Filter out `ksp` and `KaptGenerateStubsTask` from `freeCompilerArgs`.
  See [#335](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/335)
* [IMPROVEMENT] Fix gitlab-ci instrumented task.
  See [#331](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/331)
* [IMPROVEMENT] Enhance Compiler plugin binary backward compatibility.
  See [#345](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/345)
* [IMPROVEMENT] Add precise error message for missing dependencies.
  See [#361](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/361)
* [IMPROVEMENT] Only warn missing dependencies instead of throwing errors.
  See [#368](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/368)
* [IMPROVEMENT] Fix tag injection when `Modifier` is absent.
  See [#357](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/357)
* [IMPROVEMENT] Do not show Android project error if KCP is enabled.
  See [#359](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/359)
* [IMPROVEMENT] Use Sonatype snapshot URL and fix compile warnings.
  See [#367](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/367)
* [MAINTENANCE] Bump Kotlin version and embeddable to `2.0.21`.
  See [#339](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/339)
* [MAINTENANCE] Separate Compose extension to register respectively.
  See [#332](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/332)
* [MAINTENANCE] Merge `develop` into `feature/kotlin-compiler-plugin`.
  See [#355](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/355)
* [MAINTENANCE] Merge Compose instrumentation configuration into one.
  See [#358](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/358)
* [MAINTENANCE] Fix instrumented test task timeout issue.
  See [#362](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/362)
* [MAINTENANCE] Dev: Start next development iteration.
  See [#351](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/351)
* [MAINTENANCE] Datadog Gradle Plugin: Update to version `1.16.0`.
  See [#354](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/354)
* [MAINTENANCE] Merge `release/1.16.0` into `develop`.
  See [#353](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/353)
* [MAINTENANCE] Add compilation unit test for `modifier` injection.
  See [#323](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/323)
* [MAINTENANCE] Setup compiling test for `NavHost` injection.
  See [#328](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/328)
* [MAINTENANCE] Merge `develop` into `feature/kotlin-compiler-plugin`.
  See [#317](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/317)
* [MAINTENANCE] Kotlin Compiler Plugin: Add `instrumented` test module.
  See [#315](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/315)
* [MAINTENANCE] Merge `feature/kotlin-compiler-plugin` into `develop`.
  See [#360](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/360)
* [MAINTENANCE] Start to publish SNAPSHOT from `develop` branch.
  See [#366](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/366)
* [MAINTENANCE] CodeQL: Update Ubuntu version from `20.04` to `latest`.
  See [#364](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/364)
* [MAINTENANCE] Merge `develop` into `feature/kotlin-compiler-plugin`.
  See [#365](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/365)
* [MAINTENANCE] Update Compose Instrumentation functions references.
  See [#363](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/363)
* [MAINTENANCE] Merge `develop` into `feature/kotlin-compiler-plugin`.
  See [#336](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/336)
* [MAINTENANCE] Add Instrumented test for view tracking instrumentation.
  See [#341](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/341)
* [MAINTENANCE] Update the package name of instrumentation API.
  See [#343](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/343)

# 1.16.0 / 2025-04-07

* [IMPROVEMENT] Log message if there are no files to upload. See [#309](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/309)
* [IMPROVEMENT] Escape user input for mapping file packages aliases. See [#325](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/325)
* [IMPROVEMENT] Improve big mapping file handling in the shrinking process. See [#326](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/326)
* [MAINTENANCE] Next dev iteration. See [#305](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/305)
* [MAINTENANCE] Update version of Datadog Gradle Plugin used in samples to 1.15.0. See [#308](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/308)
* [MAINTENANCE] Update Datadog SDK to 2.18.0. See [#310](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/310)
* [MAINTENANCE] Update `github/codeql-action` to latest version. See [#312](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/312)
* [MAINTENANCE] Overwrite `gradle.properties` with CI configs for test plugin task. See [#316](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/316)
* [MAINTENANCE] Increase CI kubernetes memory limit to 20Gi to avoid OOM. See [#318](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/318)
* [MAINTENANCE] Remove git fetch calls from jobs. See [#319](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/319)
* [MAINTENANCE] Update AGP to 8.8.2. See [#320](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/320)
* [MAINTENANCE] Update AGP to version 8.9.0. See [#324](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/324)
* [MAINTENANCE] Update Datadog SDK to version 2.19.0. See [#327](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/327)
* [MAINTENANCE] Update Datadog SDK to version 2.19.1. See [#333](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/333)
* [MAINTENANCE] Bump Kotlin version to 2.0.21. See [#338](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/338)
* [MAINTENANCE] Update Datadog SDK to version 2.19.2. See [#340](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/340)
* [MAINTENANCE] Update `org.json:json` library version to 20231013. See [#347](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/347)
* [DOCS] Add more configuration options to `README`. See [#349](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/349)

# 1.15.0 / 2025-01-16

* [FEATURE] Support custom native symbols path. See [#302](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/302)
* [DOCS] Remove the statement from `README` that logs are not deobfuscated. See [#275](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/275)
* [DOCS] Add uploading section to README. See [#281](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/281)
* [MAINTENANCE] Next dev iteration. See [#267](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/267)
* [MAINTENANCE] Update Datadog SDK to version 2.10.1. See [#268](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/268)
* [MAINTENANCE] Use Datadog Gradle Plugin 1.14.0. See [#271](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/271)
* [MAINTENANCE] Merge `release/1.14.0` branch into `develop` branch. See [#270](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/270)
* [MAINTENANCE] Update AGP version to 8.5.0. See [#276](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/276)
* [MAINTENANCE] Update Gradle version to 8.8. See [#277](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/277)
* [MAINTENANCE] Update Datadog SDK to version 2.11.0. See [#278](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/278)
* [MAINTENANCE] Update Github token reference. See [#282](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/282)
* [MAINTENANCE] Update Datadog SDK to version 2.12.0. See [#284](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/284)
* [MAINTENANCE] Update Datadog SDK to version 2.12.1. See [#285](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/285)
* [MAINTENANCE] Update Datadog SDK to version 2.13.0. See [#286](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/286)
* [MAINTENANCE] Update Datadog SDK to version 2.13.1. See [#289](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/289)
* [MAINTENANCE] Update Datadog SDK to version 2.14.0. See [#290](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/290)
* [MAINTENANCE] Update Gradle to 8.10.2, AGP to 8.6.1 and also update latest test configuration for the functional test. See [#292](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/292)
* [MAINTENANCE] Update Datadog SDK to version 2.15.0. See [#291](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/291)
* [MAINTENANCE] Update Datadog SDK to version 2.15.1. See [#293](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/293)
* [MAINTENANCE] Update Datadog SDK to version 2.16.0. See [#295](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/295)
* [MAINTENANCE] Update Gradle to 8.11.1, AGP to 8.7.2, Kotlin to 2.1.0 for the latest versions configuration for the functional tests. See [#298](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/298)
* [MAINTENANCE] Update Gradle to 8.11.1, AGP to 8.7.2. See [#299](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/299)
* [MAINTENANCE] Update Datadog SDK to version 2.16.1. See [#300](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/300)
* [MAINTENANCE] Update Datadog SDK to version 2.17.0. See [#301](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/301)
* [MAINTENANCE] AGP 8.8.0 for the latest configuration in the functional tests. See [#303](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/303)

# 1.14.0 / 2024-05-30

* [FEATURE] Add tasks for upload NDK symbol files to Datadog. See [#220](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/220)
* [FEATURE] Support new Variant API. See [#263](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/263)
* [IMPROVEMENT] Standardize architectures. See [#249](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/249)
* [IMPROVEMENT] Emulate upload network call for functional tests. See [#255](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/255)
* [IMPROVEMENT] Mark upload task as not compatible with configuration cache. See [#256](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/256)
* [IMPROVEMENT] Avoid BuildId task creation if there is no obfuscation or native build providers registered. See [#259](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/259)
* [IMPROVEMENT] Increase upload timeout to 120s to match EvP limits. See [#261](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/261)
* [IMPROVEMENT] Add test for the duplicate resources issue with AGP 8.4.x. See [#264](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/264)
* [IMPROVEMENT] Use mapping file provider in case of legacy Variant API. See [#265](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/265)
* [MAINTENANCE] Next dev iteration. See [#233](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/233)
* [MAINTENANCE] Update version of static analysis pipeline used. See [#234](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/234)
* [MAINTENANCE] Update Datadog SDK to version 2.8.0. See [#235](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/235)
* [MAINTENANCE] Merge `release/1.13.0` branch into `develop` branch. See [#236](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/236)
* [MAINTENANCE] Use Datadog Gradle Plugin 1.13.0 in samples. See [#238](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/238)
* [MAINTENANCE] Update AGP to 8.3.2. See [#239](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/239)
* [MAINTENANCE] Merge release `1.13.1` into `develop` branch. See [#244](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/244)
* [MAINTENANCE] Use Datadog Gradle Plugin 1.13.1 for samples. See [#245](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/245)
* [MAINTENANCE] Migrate to GBI images. See [#246](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/246)
* [MAINTENANCE] Add dogfood script. See [#247](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/247)
* [MAINTENANCE] Install NDK 25.1.8937393 in the Docker image. See [#248](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/248)
* [MAINTENANCE] Redirect slack notifications to `mobile-sdk-ops` channel. See [#250](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/250)
* [MAINTENANCE] Update Gradle to 8.7, AGP to 8.4.0. See [#253](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/253)
* [MAINTENANCE] Update Datadog SDK to version 2.9.0. See [#254](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/254)
* [MAINTENANCE] Update AGP version to 8.4.1. See [#260](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/260)
* [MAINTENANCE] Update Datadog SDK to version 2.10.0. See [#262](https://github.com/DataDog/dd-sdk-android-gradle-plugin/pull/262)

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
