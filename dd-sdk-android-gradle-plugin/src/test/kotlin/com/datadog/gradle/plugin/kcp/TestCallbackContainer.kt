package com.datadog.gradle.plugin.kcp

/**
 * A container for storing callbacks passed from the unit test environment.
 *
 * This object should be loaded by the same class loader that loads the test target class file.
 * To ensure the test target function is invoked correctly, the function under test should
 * call [TestCallbackContainer.invokeCallback], allowing external verification of the callback invocation.
 */
object TestCallbackContainer {

    private var callback: () -> Unit = {}

    /**
     * Sets the callback function to be invoked during testing.
     *
     * @param callback The function to be stored and later invoked.
     */
    fun setCallback(callback: () -> Unit) {
        this.callback = callback
    }

    /**
     * Invokes the stored callback function.
     */
    fun invokeCallback() {
        callback.invoke()
    }
}
