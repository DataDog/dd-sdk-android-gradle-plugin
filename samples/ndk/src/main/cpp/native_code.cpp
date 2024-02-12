// Write C++ code here.
//
// Do not forget to dynamically load the C++ library into your application.
//
// For instance,
//
// In MainActivity.java:
//    static {
//       System.loadLibrary("ndk");
//    }
//
// Or, in MainActivity.kt:
//    companion object {
//      init {
//         System.loadLibrary("ndk")
//      }
//    }

#include <jni.h>

extern "C" JNIEXPORT jstring JNICALL
Java_com_datadog_example_ndk_MainActivity_stringFromJNI( JNIEnv* env,
                                                  jobject object ) {
    return env->NewStringUTF("Hello from JNI!");
}