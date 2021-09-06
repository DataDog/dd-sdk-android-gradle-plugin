package com.datadog.gradle.plugin

import com.datadog.gradle.plugin.internal.MissingSdkException
import com.datadog.tools.unit.setStaticValue
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.xgouchet.elmyr.Forge
import fr.xgouchet.elmyr.annotation.StringForgery
import fr.xgouchet.elmyr.junit5.ForgeConfiguration
import fr.xgouchet.elmyr.junit5.ForgeExtension
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedConfiguration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.Extensions
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import org.slf4j.Logger

@Extensions(
    ExtendWith(MockitoExtension::class),
    ExtendWith(ForgeExtension::class)
)
@MockitoSettings(strictness = Strictness.LENIENT)
@ForgeConfiguration(Configurator::class)
internal class DdCheckSdkDepsTaskTest {
    lateinit var testedTask: DdCheckSdkDepsTask

    lateinit var fakeSdkCheckLevel: SdkCheckLevel

    @Mock
    lateinit var mockConfiguration: Configuration

    @StringForgery
    lateinit var fakeVariantName: String

    @TempDir
    lateinit var tempDir: File

    @Mock
    lateinit var mockLogger: Logger

    lateinit var originalLogger: Logger

    @Mock
    lateinit var mockResolvedConfiguration: ResolvedConfiguration

    @BeforeEach
    fun `set up`(forge: Forge) {
        originalLogger = DdAndroidGradlePlugin.LOGGER
        DdAndroidGradlePlugin::class.java.setStaticValue("LOGGER", mockLogger)

        whenever(mockConfiguration.resolvedConfiguration).thenReturn(mockResolvedConfiguration)
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(emptySet())
        fakeSdkCheckLevel = forge.aValueFrom(SdkCheckLevel::class.java)
        val fakeProject = ProjectBuilder.builder()
            .withProjectDir(tempDir)
            .build()

        testedTask = fakeProject.tasks.create(
            "DdCheckDepsTask",
            DdCheckSdkDepsTask::class.java
        ) {
            it.configuration.set(mockConfiguration)
            it.sdkCheckLevel.set(fakeSdkCheckLevel)
            it.variantName.set(fakeVariantName)
        }
    }

    @AfterEach
    fun `tear down`() {
        DdAndroidGradlePlugin::class.java.setStaticValue("LOGGER", originalLogger)
    }

    // region taskAction

    @Test
    fun `𝕄 throw exception 𝕎 sdk dependency could not be found { sdkCheckLevel = FAIL }()`(
        forge: Forge
    ) {

        // GIVEN
        val dependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(
            dependencies.toSet()
        )
        testedTask.sdkCheckLevel.set(SdkCheckLevel.FAIL)

        // THEN
        assertThatThrownBy { testedTask.applyTask() }.isInstanceOf(MissingSdkException::class.java)
    }

    @Test
    fun `𝕄 log a warning 𝕎 sdk dependency could not be found { sdkCheckLevel = WARN }()`(
        forge: Forge
    ) {

        // GIVEN
        val dependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }
        whenever(mockResolvedConfiguration.firstLevelModuleDependencies).thenReturn(
            dependencies.toSet()
        )
        testedTask.sdkCheckLevel.set(SdkCheckLevel.WARN)

        // WHEN
        testedTask.applyTask()

        // THEN
        verify(mockLogger).warn(DdCheckSdkDepsTask.MISSING_DD_SDK_MESSAGE.format(fakeVariantName))
    }

    // endregion

    // region isDatadogDependencyPresent

    @Test
    fun `𝕄 return true 𝕎 isDatadogDependencyPresent() { sdk is at the top level }`(
        forge: Forge
    ) {

        val sdkDependency = mock<ResolvedDependency>()
        whenever(sdkDependency.moduleName) doReturn "dd-sdk-android"
        whenever(sdkDependency.moduleGroup) doReturn "com.datadoghq"

        val otherDependencies = forge.aList {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val allDependencies = (otherDependencies + sdkDependency).shuffled().toSet()

        assertThat(
            testedTask.isDatadogDependencyPresent(allDependencies)
        ).isTrue()
    }

    @Test
    fun `𝕄 return true 𝕎 isDatadogDependencyPresent() { sdk is at the child level }`(
        forge: Forge
    ) {

        val sdkDependency = mock<ResolvedDependency>()
        whenever(sdkDependency.moduleName) doReturn "dd-sdk-android"
        whenever(sdkDependency.moduleGroup) doReturn "com.datadoghq"

        val fakeDependencyGenerator: Forge.() -> ResolvedDependency = {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val childDependencies = forge.aList(forging = fakeDependencyGenerator)
        val topDependencies = forge.aList(forging = fakeDependencyGenerator)
        whenever(topDependencies.random().children) doReturn (childDependencies + sdkDependency)
            .shuffled().toSet()

        assertThat(
            testedTask.isDatadogDependencyPresent(topDependencies.toSet())
        ).isTrue()
    }

    @Test
    fun `𝕄 return false 𝕎 isDatadogDependencyPresent() { sdk is not there }`(
        forge: Forge
    ) {
        val fakeDependencyGenerator: Forge.() -> ResolvedDependency = {
            val dependency = mock<ResolvedDependency>()
            whenever(dependency.moduleName) doReturn forge.anAsciiString()
            whenever(dependency.moduleGroup) doReturn forge.anAsciiString()
            dependency
        }

        val childDependencies = forge.aList(forging = fakeDependencyGenerator)
        val topDependencies = forge.aList(forging = fakeDependencyGenerator)

        whenever(topDependencies.random().children) doReturn childDependencies.toSet()

        assertThat(
            testedTask.isDatadogDependencyPresent(topDependencies.toSet())
        ).isFalse()
    }

    // endregion
}
