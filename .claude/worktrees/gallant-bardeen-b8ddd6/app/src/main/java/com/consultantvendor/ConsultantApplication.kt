package com.consultantvendor

import android.app.Application
import android.util.Log
import androidx.lifecycle.*
import com.appsflyer.AppsFlyerLib
import com.appsflyer.attribution.AppsFlyerRequestListener
import com.consultantvendor.data.models.requests.AppFeatures
import com.consultantvendor.data.models.responses.appdetails.AppVersion
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.di.DaggerAppComponent
import com.consultantvendor.utils.AppSocket
import com.consultantvendor.utils.PrefsManager
import com.facebook.drawee.backends.pipeline.Fresco
import com.google.android.libraries.places.api.Places
import dagger.android.AndroidInjector
import dagger.android.DaggerApplication
import javax.inject.Inject


var appFeatures = AppFeatures()
var appClientDetails = AppVersion()

class ConsultantApplication : DaggerApplication(), LifecycleObserver {


    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var appSocket: AppSocket

    private var isReceiverRegistered = false


    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        Fresco.initialize(this)
        setsApplication(this)
        appSocket.init()

        appClientDetails = userRepository.getAppSetting()

        // Initialize Places.
        try {
            Places.initialize(applicationContext, getString(R.string.google_places_api_key))
        } catch (e: Exception) {
        }

        /*Check for features*/
        when (BuildConfig.FLAVOR) {
            "consult", "edu", "marketplace", "healthcare" -> {
                appFeatures.needLanguageScreen = true
                appFeatures.needWalkThrough = true
                appFeatures.needUserDoctorScreen = false
                appFeatures.needLocation = true
                appFeatures.needClasses = true
                appFeatures.needBlogs = true
                appFeatures.needArticles = true
                appFeatures.needHealthTools = false
                appFeatures.freeExpertAdvise = true
            }
            "heal" -> {
                appFeatures.needLanguageScreen = true
                appFeatures.needWalkThrough = true
                appFeatures.needUserDoctorScreen = true
                appFeatures.needLocation = true
                appFeatures.needArticles = true
                appFeatures.needClasses = true
                appFeatures.needHealthTools = true
                appFeatures.signUpAddition = true
                appFeatures.needInsurance = false
                appFeatures.freeExpertAdvise = true
            }
            "homeDoctor" -> {
                appFeatures.needLanguageScreen = true
                appFeatures.needLocation = true
                AppsFlyerLib.getInstance().init(getString(R.string.apps_flyer_dev_key), null, this)
                AppsFlyerLib.getInstance().start(applicationContext,null,object : AppsFlyerRequestListener{
                    override fun onSuccess() {
                        Log.d("AppsFlyer", "Launch sent successfully, got 200 response code from server");
                    }

                    override fun onError(i: Int, s: String) {
                        Log.d("AppsFlyer", "Launch failed to be sent:\n" +
                                "Error code: " + i + "\n"
                                + "Error description: " + s)
                    }
                })

            }
            "airdoc" -> {
                appFeatures.needLocation = true
            }
            "nurseLynx" -> {
                appFeatures.needLocation = true
                appFeatures.needInviteCode = true
            }
            "taradoc" -> {
                appFeatures.needLanguageScreen = false
                appFeatures.needLocation = true
            }
            "clouddoc" -> {
                appFeatures.needLocation = true
                appFeatures.needBlogs = true
            }
            "meetMd" -> {
                appFeatures.needLocation = true
            }
            else -> {
                appFeatures.needClasses = true
                appFeatures.needBlogs = true
                appFeatures.needArticles = true
            }
        }
    }

    override fun applicationInjector(): AndroidInjector<out DaggerApplication> =
            DaggerAppComponent.builder().create(this)

    companion object {
        var isApplication: Application? = null

        fun setsApplication(sApplication: Application) {
            isApplication = sApplication
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun appInResumeState() {
        //Toast.makeText(this, "In Foreground", Toast.LENGTH_LONG).show();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun appInPauseState() {
        //Toast.makeText(this, "In Background", Toast.LENGTH_LONG).show();
    }
}