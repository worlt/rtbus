package com.trackr.service

import android.media.AudioAttributes
import android.os.VibrationAttributes
import android.os.Vibrator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.robolectric.Shadows.shadowOf
import org.robolectric.util.ReflectionHelpers

/** On API 33+, Android converts the AudioAttributes overload before the shadow records it. */
internal fun assertNavigationVibrationAttributes(vibrator: Vibrator) {
    val attributes = shadowOf(vibrator).getVibrationAttributesFromLastVibration() as? VibrationAttributes
    assertNotNull("System must retain the converted vibration attributes", attributes)
    assertEquals(VibrationAttributes.USAGE_COMMUNICATION_REQUEST, attributes!!.usage)
    assertEquals(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE,
        ReflectionHelpers.callInstanceMethod<Int>(attributes, "getAudioUsage"))
}
