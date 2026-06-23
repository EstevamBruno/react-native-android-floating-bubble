package com.androidfloatingbubble

import com.facebook.react.bridge.ReactApplicationContext

class AndroidFloatingBubbleModule(reactContext: ReactApplicationContext) :
  NativeAndroidFloatingBubbleSpec(reactContext) {

  override fun multiply(a: Double, b: Double): Double {
    return a * b
  }

  companion object {
    const val NAME = NativeAndroidFloatingBubbleSpec.NAME
  }
}
