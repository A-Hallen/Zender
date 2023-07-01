package com.hallen.zender.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import javax.inject.Inject

class AppsUseCase @Inject constructor() {
    fun getAllApps(context: Context): List<ApplicationInfo> {
        val nonSystemApps = mutableListOf<ApplicationInfo>()
        val packageManager = context.packageManager
        val applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in applications) {
            if (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 1) {
                // System application
            } else {
                nonSystemApps.add(appInfo)
            }
        }

        nonSystemApps.sortBy { it.loadLabel(packageManager).toString().lowercase() }
        return nonSystemApps
    }
}
