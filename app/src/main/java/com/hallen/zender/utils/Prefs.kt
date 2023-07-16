package com.hallen.zender.utils

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo

class Prefs(context: Context) {
    private val SHARED_NAME = "Zender"
    private val storage: SharedPreferences = context.getSharedPreferences(SHARED_NAME, 0)

    fun setDefaultApp(mime: String, resolveInfo: ResolveInfo) {
        val activity: ActivityInfo = resolveInfo.activityInfo
        val packageName: String = activity.applicationInfo.packageName
        val activityName: String = activity.name
        storage.edit().putString(mime, packageName).apply()
        storage.edit().putString(mime + "activity", activityName).apply()
    }

    fun getDefaultApp(mime: String): Array<String> {
        val a = storage.getString(mime, "")
        val b = storage.getString(mime + "activity", "")
        return if (a == "") {
            arrayOf("", "")
        } else {
            arrayOf(a!!, b!!)
        }
    }
}