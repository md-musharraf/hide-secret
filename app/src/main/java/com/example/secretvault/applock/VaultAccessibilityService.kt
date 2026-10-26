package com.example.secretvault.applock

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

class VaultAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val pkgName = event.packageName?.toString() ?: return
            if (pkgName == packageName) return // Do not lock ourselves

            val appLockManager = AppLockManager.getInstance(this)
            if (appLockManager.isAppLocked(pkgName)) {
                if (!appLockManager.isSessionUnlocked(pkgName)) {
                    val isFakeCrash = appLockManager.isFakeCrashEnabled(pkgName)
                    AppLockOverlayActivity.start(this, pkgName, isFakeCrash)
                }
            }
        }
    }

    override fun onInterrupt() {}
}
