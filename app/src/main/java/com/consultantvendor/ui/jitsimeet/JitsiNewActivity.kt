package com.consultantvendor.ui.jitsimeet

import android.Manifest
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
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
import timber.log.Timber
import java.net.MalformedURLException
import java.net.URL
import javax.inject.Inject


class JitsiNewActivity: DaggerAppCompatActivity(), JitsiMeetActivityInterface {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    private var isReceiverRegistered = false

    private var jitsiMeetView: JitsiMeetView? = null

    private var jitsiClass: JitsiClass? = null

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        //setContentView(R.layout.activity_jitsi)

        checkPermission()

        onBackPressedDispatcher.addCallback(this) {
           openDialog()
        }

    }

    private fun openDialog(){
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.end_call_title))
            .setMessage(getString(R.string.end_call_message))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->

                if (isConnectedToInternet(this, true) && jitsiClass?.isClass == false) {
                    userRepository.callStatus(
                        jitsiClass?.id ?: "", jitsiClass?.call_id ?: "",
                        PushType.CALL_CANCELED
                    )

                    // Tell Jitsi to hang up
                    val hangupIntent = Intent("org.jitsi.meet.HANG_UP")
                    LocalBroadcastManager.getInstance(this).sendBroadcast(hangupIntent)

                    // Dispose of the Jitsi view
                    jitsiMeetView?.dispose()

                    // Finish the activity
                    finish()
                    longToast(getString(R.string.call_ended))
                }

                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun intialise() {
        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

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
            .setFeatureFlag("invite.enabled", false)
            .setFeatureFlag("chat.enabled", false)
            .setFeatureFlag("calendar.enabled", false)
            .setFeatureFlag("call-integration.enabled", false)
            .setFeatureFlag("live-streaming.enabled", false)
            .setFeatureFlag("recording.enabled", false)
            .setFeatureFlag("tile-view.enabled", false)
            .setFeatureFlag("fullscreen.enabled", false)
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

            val setAudioOnly =
                jitsiClass?.callType?.lowercase() == ConsultType.AUDIO_CALL || jitsiClass?.callType?.lowercase() == ConsultType.CALL

            Log.e("TAG", "intialiseJitsi: " + jitsiClass?.callType?.lowercase())

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

            jitsiMeetView?.join(options)

            setContentView(jitsiMeetView)
            jitsiMeetView?.let(::applyInsets)

            SoundPoolManager.getInstance(this)?.stopRinging()
        }
    }


    override fun requestPermissions(p0: Array<String>, p1: Int, p2: PermissionListener?) {
        JitsiMeetActivityDelegate.requestPermissions(this, p0, p1, p2)
    }

    companion object {
        private const val TAG = "JitsiActivity"
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        JitsiMeetActivityDelegate.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    val jitsiBroadCast = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                BroadcastEvent.Type.CONFERENCE_JOINED.action -> {
                    Log.d(TAG, "onReceive: joined call  ")
                    val list: List<Pair<String, Any?>>? = intent?.extras?.keySet()?.mapNotNull {
                        it to intent?.extras?.get(it)
                    }
                    val map2: Map<String?, Any?> = buildMap {
                        list?.forEach {
                            put(it.first, it.second)
                        }
                    }
                    onConferenceJoined(map2)
                }

                BroadcastEvent.Type.CONFERENCE_TERMINATED.action -> {
                    Log.d(TAG, "onReceive: terminated call")
                    val list: List<Pair<String, Any?>>? = intent?.extras?.keySet()?.mapNotNull {
                        it to intent.extras?.get(it)
                    }
                    val map2: Map<String?, Any?> = buildMap {
                        list?.forEach {
                            put(it.first, it.second)
                        }
                    }
                    onConferenceTerminated(map2)
                }

                BroadcastEvent.Type.PARTICIPANT_JOINED.action -> {
                    // For participant events, the data is usually in a Serializable extra
                    val data = intent?.getSerializableExtra("data") as? kotlin.collections.HashMap<String, Any>
                    longToast("Participant joining")
                }

                BroadcastEvent.Type.AUDIO_MUTED_CHANGED.action -> {
                    val data = intent?.getSerializableExtra("data") as? kotlin.collections.HashMap<String, Any>
                    data?.let {
                        val participantId = it["participantId"] as? String
                        val muted = it["muted"] as? Boolean
                         Log.d("JitsiEvents", "Participant $participantId Audio Muted: $muted")
                    }
                }

                BroadcastEvent.Type.VIDEO_MUTED_CHANGED.action -> {
                    val data = intent?.getSerializableExtra("data") as? kotlin.collections.HashMap<String, Any>
                    data?.let {
                        val participantId = it["participantId"] as? String
                        val muted = it["muted"] as? Boolean
                         Log.d("JitsiEvents", "Participant $participantId Video Muted: $muted")
                    }
                }
                // Add more `when` branches for other BroadcastEvent.Type actions as needed
                else -> {
                    // Log.d("JitsiEvents", "Unhandled Broadcast: ${intent.action}")
                }
            }
        }
    }

    fun registerJitsiBroadCastReceiver() {
        val intentFilter = IntentFilter()
        // Add the actions you want to listen for
        intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_JOINED.action)
        intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_TERMINATED.action)
        intentFilter.addAction(BroadcastEvent.Type.CONFERENCE_WILL_JOIN.action)
        intentFilter.addAction(BroadcastEvent.Type.PARTICIPANT_JOINED.action)
        intentFilter.addAction(BroadcastEvent.Type.PARTICIPANT_LEFT.action)
        intentFilter.addAction(BroadcastEvent.Type.AUDIO_MUTED_CHANGED.action)
        intentFilter.addAction(BroadcastEvent.Type.VIDEO_MUTED_CHANGED.action)
        intentFilter.addAction(BroadcastEvent.Type.SCREEN_SHARE_TOGGLED.action)


        LocalBroadcastManager.getInstance(this).registerReceiver(jitsiBroadCast, intentFilter)
    }

    fun onConferenceJoined(data: Map<String?, Any?>) {
        val hashMap = hashMapOf(*data.map { it.toPair() }.toTypedArray())
        JitsiMeetLogger.i("Conference joined: $data")
        JitsiMeetOngoingConferenceService.launch(this, hashMap);
    }

    fun onConferenceTerminated(data: Map<String?, Any?>) {
        JitsiMeetLogger.i("Conference terminated: $data")

        if (isConnectedToInternet(this, true) && jitsiClass?.isClass == false) {
            userRepository.callStatus(
                jitsiClass?.id ?: "", jitsiClass?.call_id ?: "",
                PushType.CALL_CANCELED
            )
            longToast(getString(R.string.call_ended))
        }

        jitsiMeetView?.dispose()
        finish()
    }

    fun onConferenceWillJoin(data: Map<String?, Any?>) {
        JitsiMeetLogger.i("Conference will join: $data")
    }

    val audioPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {   result ->

            if (result.all {
                    it.value
                }) {
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
                if (ActivityCompat.checkSelfPermission(
                        this,
                        permission!!
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return false
                }
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
        registerJitsiBroadCastReceiver()
        try {
            JitsiMeetActivityDelegate.onHostResume(this)
        } catch (e: Exception) {
        }
    }

    override fun onStop() {
        super.onStop()
        JitsiMeetActivityDelegate.onHostPause(this)
    }

    override fun onDestroy() {
        unregisterReceiver()
        JitsiMeetActivityDelegate.onHostDestroy(this)
        jitsiMeetView?.dispose()
        Log.d(TAG, "onDestroy: ondestroy called")
        super.onDestroy()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(Constants.ACTION_INCOMING_CALL)
            intentFilter.addAction(Constants.ACTION_CANCEL_CALL)
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            LocalBroadcastManager.getInstance(this).registerReceiver(
                callCancelledReceiver, intentFilter
            )
            isReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        kotlin.runCatching {
            JitsiMeetActivityDelegate.onHostPause(this)
        }
        Timber.tag(TAG).d("onPause: called ")
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(callCancelledReceiver)
            isReceiverRegistered = false
            runCatching {
                LocalBroadcastManager.getInstance(this).unregisterReceiver(jitsiBroadCast)
            }
        }
    }

    private val callCancelledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(EXTRA_REQUEST_ID) == jitsiClass?.call_id) {
                if (intent.action == Constants.ACTION_CANCEL_CALL || intent.action == PushType.REQUEST_COMPLETED) {
                    jitsiMeetView?.dispose()
                    finish()
                }
            }
        }
    }

}