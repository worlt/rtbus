package com.trackr.service

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.trackr.core.model.TrackingSessionState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ReminderFeedbackLogTest {
    @Test fun historyIsBoundedAndContainsNoRouteVehicleOrLocationDetails() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val state = TrackingSessionState(lineName = "private-route", targetStopName = "private-stop",
            pinnedBusId = "private-id", pinnedPlate = "private-plate", targetStopLat = 31.123,
            targetStopLng = 121.123, stopsRemaining = 0, etaSeconds = 30)
        repeat(8) { ReminderFeedbackLog.record(context, "EVENT_$it", true, ReminderVibration.Result.REQUESTED, state) }
        ReminderFeedbackLog.tracking(context, state)
        val report = ReminderFeedbackLog.report(context)
        assertEquals(6, Regex("EVENT_[0-9]").findAll(report).count())
        assertFalse(report.contains("EVENT_0"))
        assertFalse(report.contains("EVENT_1"))
        assertTrue(report.contains("EVENT_7"))
        assertFalse(report.contains("private-"))
        assertFalse(report.contains("31.123"))
        assertFalse(report.contains("121.123"))
        ReminderFeedbackLog.tracking(context, null, completed = true)
        assertTrue(ReminderFeedbackLog.report(context).contains("已自动结束"))
    }
}
