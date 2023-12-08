package com.datadog.gradle.plugin.utils.assertj

import com.datadog.gradle.plugin.DdAndroidGradlePlugin
import org.assertj.core.api.AbstractAssert
import org.assertj.core.api.Assertions.assertThat
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome

internal class BuildResultAssert(actual: BuildResult) :
    AbstractAssert<BuildResultAssert, BuildResult>(actual, BuildResultAssert::class.java) {

    fun containsInOutput(text: String): BuildResultAssert {
        assertThat(actual.output).contains(text)
        return this
    }

    fun hasSuccessfulTaskOutcome(taskName: String): BuildResultAssert {
        val task = actual.task(taskName)
        assertThat(task)
            .overridingErrorMessage(
                "Expected to have task with name=$taskName to" +
                    " be present in execution graph, but it wasn't found there."
            )
            .isNotNull

        assertThat(task?.outcome)
            .overridingErrorMessage(
                "Expected task with name=$taskName" +
                    " to have outcome=${TaskOutcome.SUCCESS}, but the actual outcome was ${task?.outcome}"
            )
            .isEqualTo(TaskOutcome.SUCCESS)

        return this
    }

    fun hasNoUploadTasks(): BuildResultAssert {
        assertThat(actual.tasks).noneMatch {
            it.path.contains(DdAndroidGradlePlugin.UPLOAD_TASK_NAME)
        }
        return this
    }

    companion object {

        fun assertThat(actual: BuildResult) = BuildResultAssert(actual)
    }
}
