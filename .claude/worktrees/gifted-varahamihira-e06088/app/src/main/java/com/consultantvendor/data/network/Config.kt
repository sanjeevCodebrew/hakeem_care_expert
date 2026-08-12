package com.consultantvendor.data.network

import com.consultantvendor.BuildConfig

object Config {

    var BASE_URL = ""

    private val appMode = AppMode.DEV

    val baseURL: String
        get() {
            init(appMode)
            return BASE_URL
        }

    private fun init(appMode: AppMode) {

        BASE_URL = when (appMode) {
            AppMode.DEV -> {
                BuildConfig.BASE_URL
            }
            AppMode.LIVE -> {
                BuildConfig.BASE_URL
            }
        }
    }

    private enum class AppMode {
        DEV, LIVE
    }
}