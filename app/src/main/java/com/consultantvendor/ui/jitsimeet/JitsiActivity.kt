package com.consultantvendor.ui.jitsimeet

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.models.responses.JitsiClass
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.ui.calling.Constants
import com.consultantvendor.ui.calling.SoundPoolManager
import com.consultantvendor.utils.*
import com.facebook.react.modules.core.PermissionListener
import dagger.android.support.DaggerAppCompatActivity
import org.jitsi.meet.sdk.*
import org.jitsi.meet.sdk.log.JitsiMeetLogger
import java.net.MalformedURLException
import java.net.URL
import java.util.HashMap
import javax.inject.Inject


class JitsiActivity : DaggerAppCompatActivity(), JitsiMeetActivityInterface{

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    private var isReceiverRegistered = false

    private var jitsiMeetView: JitsiMeetView? = null

    private var jitsiClass: JitsiClass? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super<DaggerAppCompatActivity>.onCreate(savedInstanceState)
//        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_jitsi)

        regiseterLocalBraodCast()
        checkPermission()

    }

    val boradCasr = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "onReceive: broadCast receiver called")
            when (intent?.data.toString()) {
                BroadcastEvent.Type.CONFERENCE_JOINED.action -> {
                    Log.d(TAG, "onReceive: joined call  ")
                    val list: List<Pair<String, Any?>>? =intent?.extras?.keySet()?.mapNotNull {
                        it to intent?.extras?.get(it)
                    }
                    val map2: Map<String?, Any?> =buildMap {
                        list?.forEach {
                            put(it?.first,it?.second)
                        }
                    }
                    onConferenceJoined(map2)
                }

                BroadcastEvent.Type.CONFERENCE_TERMINATED.action -> {
//                  onConferenceTerminated(intent.extras)
                    Log.d(TAG, "onReceive: terminated call")
                    val list: List<Pair<String, Any?>>? =intent?.extras?.keySet()?.mapNotNull {
                        it to intent?.extras?.get(it)
                    }
                    val map2: Map<String?, Any?> =buildMap {
                        list?.forEach {
                            put(it?.first,it?.second)
                        }
                    }
                    onConferenceTerminated(map2)
                }
            }
        }
    }

    private fun regiseterLocalBraodCast() {
        val intentFilter = IntentFilter();
        intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_JOINED.getAction());
        intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_TERMINATED.getAction());
        LocalBroadcastManager.getInstance(this@JitsiActivity)
            .registerReceiver(boradCasr, intentFilter);
    }

    private fun intialise() {

        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)

        jitsiMeetView = JitsiMeetView(this)

        jitsiClass = intent.getSerializableExtra(EXTRA_CALL_NAME) as JitsiClass
        val roomName: String
        val subjectName: String

        if (jitsiClass?.isClass == false) {
            roomName = "Call_${appClientDetails.jitsi_id}_${jitsiClass?.id}"
            subjectName = "Call"
        } else {
            roomName = "Class_${appClientDetails.jitsi_id}_${jitsiClass?.id}"
            subjectName = jitsiClass?.name ?: ""
        }

        //longToast("$roomName,$subjectName")


        // Initialize default options for Jitsi Meet conferences.
        val serverURL: URL = try {
            URL(appClientDetails.jitsi_meet_url)

        } catch (e: MalformedURLException) {
            e.printStackTrace()
            throw RuntimeException("Invalid server URL!")
        }
        val defaultOptions = JitsiMeetConferenceOptions.Builder()
            .setServerURL(serverURL)
//                .setWelcomePageEnabled(false)
            .setFeatureFlag("invite.enabled", false)
            .setFeatureFlag("chat.enabled", false)
            .setFeatureFlag("calendar.enabled", false)
            .setFeatureFlag("call-integration.enabled", false)
            .setFeatureFlag("live-streaming.enabled", false)
            .setFeatureFlag("recording.enabled", false)
            .setFeatureFlag("tile-view.enabled", false)
            .setFeatureFlag("meeting-password.enabled", false)
            .setFeatureFlag("pip.enabled", true)
            .setFeatureFlag("prejoinpage.enabled", false)
            .setFeatureFlag("close-captions.enabled", false)
            .build()
        JitsiMeet.setDefaultConferenceOptions(defaultOptions)

        if (roomName.isNotEmpty()) {
            // Build options object for joining the conference. The SDK will merge the default
            // one we set earlier and this one when joining.
            val userInfo = JitsiMeetUserInfo()
            val userData = userRepository.getUser()
            userInfo.displayName = userData?.name
            userInfo.avatar = URL(getImageBaseUrl(ImageFolder.UPLOADS, userData?.profile_image))

            val setAudioOnly = jitsiClass?.callType?.lowercase() == ConsultType.AUDIO_CALL || jitsiClass?.callType?.lowercase() == ConsultType.CALL

            Log.e("TAG", "intialiseJitsi: "+jitsiClass?.callType?.lowercase() )

            val options = JitsiMeetConferenceOptions.Builder()
                .setUserInfo(userInfo)
                .setRoom(roomName)
                .setSubject(subjectName)
                .setAudioOnly(setAudioOnly)
                .build()
            // Launch the new activity with the given options. The launch() method takes care
            // of creating the required Intent and passing the options.
            /* JitsiMeetActivity.launch(this, options)
             finish()*/

//            jitsiMeetView?.join(options)
            JitsiMeetActivity.launch(this, options);


            setContentView(jitsiMeetView)
//            jitsiMeetView?.listener = this


            SoundPoolManager.getInstance(this)?.stopRinging()
        }

    }

    override fun onBackPressed() {
        super<DaggerAppCompatActivity>.onBackPressed()
    }


    override fun requestPermissions(p0: Array<out String>?, p1: Int, p2: PermissionListener?) {
        JitsiMeetActivityDelegate.requestPermissions(this, p0, p1, p2)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super<DaggerAppCompatActivity>.onRequestPermissionsResult(requestCode, permissions, grantResults)
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    fun onConferenceJoined(data: Map<String?, Any?>) {
        JitsiMeetLogger.i("Conference joined: $data")
        // Launch the service for the ongoing notification.
        // JitsiMeetOngoingConferenceService.launch(this);
    }

    fun onConferenceTerminated(data: Map<String?, Any?>) {
        JitsiMeetLogger.i("Conference terminated: $data")

        if (isConnectedToInternet(this, true) && jitsiClass?.isClass == false) {
            userRepository.callStatus(jitsiClass?.id ?: "", jitsiClass?.call_id ?: "",
                    PushType.CALL_CANCELED)

            longToast(getString(R.string.disconnecting))
        }
//
//        jitsiMeetView?.listener = null
//        jitsiMeetView?.leave()
        finish()
    }

    fun onConferenceWillJoin(data: Map<String?, Any?>) {
        JitsiMeetLogger.i("Conference will join: $data")
    }

    val audioPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.all {
                it.value
            }){
            intialise()
        }
    }

    private fun checkPermission() {
        val PERMISSIONS = arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.CAMERA)
//        if (!hasPermissions(*PERMISSIONS)) {
//            ActivityCompat.requestPermissions(this, PERMISSIONS, 100)
//        }
        audioPermissions.launch(PERMISSIONS)
    }

    fun hasPermissions(vararg permissions: String?): Boolean {
        if (permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(this, permission!!) != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }

    override fun onResume() {
        super<DaggerAppCompatActivity>.onResume()
        registerReceiver()
        try {
            JitsiMeetActivityDelegate.onHostResume(this)
        } catch (_: Exception){

        }
    }

    override fun onStop() {
        super<DaggerAppCompatActivity>.onStop()
        JitsiMeetActivityDelegate.onHostPause(this)
    }

    override fun onDestroy() {
        super<DaggerAppCompatActivity>.onDestroy()
        unregisterReceiver()
        JitsiMeetActivityDelegate.onHostDestroy(this)
//        jitsiMeetView?.leave()
        endJitsiCall()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Constants.ACTION_INCOMING_CALL)
            intentFilter.addAction(Constants.ACTION_CANCEL_CALL)
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            LocalBroadcastManager.getInstance(this).registerReceiver(callCancelledReceiver, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(callCancelledReceiver)
            isReceiverRegistered = false
        }
    }

    private val callCancelledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(EXTRA_REQUEST_ID) == jitsiClass?.call_id) {
                if (intent.action == Constants.ACTION_CANCEL_CALL || intent.action == PushType.REQUEST_COMPLETED) {
//                    jitsiMeetView?.listener = null
//                    jitsiMeetView?.leave()
                      endJitsiCall()
                }
            }
        }
    }

    private fun endJitsiCall() {
        try {
            jitsiMeetView?.abort()
            jitsiMeetView?.dispose()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        JitsiMeetActivityDelegate.onHostDestroy(this)
        finish()
    }


    companion object {
        const val TAG = "JitsiActivity"
    }

}