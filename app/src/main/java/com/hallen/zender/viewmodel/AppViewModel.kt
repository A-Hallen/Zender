package com.hallen.zender.viewmodel

import android.content.Context
import android.content.pm.ApplicationInfo
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.orhanobut.logger.Logger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@HiltViewModel
class AppViewModel @Inject constructor(private val appsUseCase: AppsUseCase) : ViewModel() {
    val appModel = MutableLiveData<List<ApplicationInfo>>()

    fun getApps(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val time = measureTimeMillis {
                val apps = appsUseCase.getAllApps(context)
                appModel.postValue(apps)
            }
            Logger.i(time.toString())
        }
    }
}