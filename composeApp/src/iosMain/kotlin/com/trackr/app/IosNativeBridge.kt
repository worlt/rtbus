package com.trackr.app

internal var publishIosSnapshot: ((String) -> Unit)? = null
internal var iosActivitiesAllowed: (() -> Boolean)? = null
fun installIosSnapshots(publish: (String) -> Unit, activitiesAllowed: () -> Boolean) {
    publishIosSnapshot = publish
    iosActivitiesAllowed = activitiesAllowed
}
