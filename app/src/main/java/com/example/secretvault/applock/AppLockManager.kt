package com.example.secretvault.applock

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import com.example.secretvault.model.LockedAppInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppLockManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("vault_app_lock_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LOCKED_PACKAGES = "locked_packages"
        private const val KEY_FAKE_CRASH_PACKAGES = "fake_crash_packages"

        @Volatile
        private var INSTANCE: AppLockManager? = null

        fun getInstance(context: Context): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLockManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    suspend fun getInstalledApps(): List<LockedAppInfo> = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = pm.queryIntentActivities(intent, 0)
        val lockedSet = getLockedPackages()
        val fakeCrashSet = getFakeCrashPackages()

        val list = mutableListOf<LockedAppInfo>()
        for (info in resolveInfos) {
            val pkg = info.activityInfo.packageName
            if (pkg == context.packageName) continue // Skip this app

            val label = info.loadLabel(pm).toString()
            list.add(
                LockedAppInfo(
                    packageName = pkg,
                    appName = label,
                    isLocked = lockedSet.contains(pkg),
                    fakeCrashEnabled = fakeCrashSet.contains(pkg)
                )
            )
        }
        list.distinctBy { it.packageName }.sortedWith(
            compareByDescending<LockedAppInfo> { it.isLocked }.thenBy { it.appName }
        )
    }

    fun getLockedPackages(): Set<String> {
        return prefs.getStringSet(KEY_LOCKED_PACKAGES, emptySet()) ?: emptySet()
    }

    fun getFakeCrashPackages(): Set<String> {
        return prefs.getStringSet(KEY_FAKE_CRASH_PACKAGES, emptySet()) ?: emptySet()
    }

    fun toggleAppLock(packageName: String, lock: Boolean) {
        val current = getLockedPackages().toMutableSet()
        if (lock) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_LOCKED_PACKAGES, current).apply()
    }

    fun toggleFakeCrash(packageName: String, enable: Boolean) {
        val current = getFakeCrashPackages().toMutableSet()
        if (enable) {
            current.add(packageName)
        } else {
            current.remove(packageName)
        }
        prefs.edit().putStringSet(KEY_FAKE_CRASH_PACKAGES, current).apply()
    }

    private val sessionUnlockedPackages = mutableSetOf<String>()

    fun unlockForSession(packageName: String) {
        synchronized(sessionUnlockedPackages) {
            sessionUnlockedPackages.add(packageName)
        }
    }

    fun isSessionUnlocked(packageName: String): Boolean {
        synchronized(sessionUnlockedPackages) {
            return sessionUnlockedPackages.contains(packageName)
        }
    }

    fun clearSessionUnlocks() {
        synchronized(sessionUnlockedPackages) {
            sessionUnlockedPackages.clear()
        }
    }

    fun isAccessibilityServiceEnabled(): Boolean {
        val expectedServiceName = "${context.packageName}/com.example.secretvault.applock.VaultAccessibilityService"
        val enabledServices = android.provider.Settings.Secure.getString(
            context.contentResolver,
            android.provider.Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        val colonSplitter = android.text.TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedServiceName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    fun isAppLocked(packageName: String): Boolean {
        return getLockedPackages().contains(packageName)
    }

    fun isFakeCrashEnabled(packageName: String): Boolean {
        return getFakeCrashPackages().contains(packageName)
    }
}
