package com.consultantvendor.ui.aghora

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.WindowManager
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.models.responses.JitsiClass
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityAghoraBinding
import com.consultantvendor.ui.calling.SoundPoolManager
import com.consultantvendor.utils.ConsultType
import com.consultantvendor.utils.EXTRA_CALL_NAME
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.LocaleHelper
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.isConnectedToInternet
import dagger.android.support.DaggerAppCompatActivity
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.video.VideoCanvas
import javax.inject.Inject


class AghoraNewActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository
    @Inject lateinit var prefsManager: PrefsManager

    private var isReceiverRegistered = false
    private var rtcEngine: RtcEngine? = null
    private var jitsiClass: JitsiClass? = null

    private var isMuted = false
    private var isVideoMuted = false

    private lateinit var binding: ActivityAghoraBinding

    private val aghoraAppId = "b25e3d53ae804174a0c341c3dcaa25cd"

    companion object {
        private const val TAG = "AgoraExpertActivity"
    }

    private val audioPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.all { it.value }) {
                initialiseAgora()
            }
        }

    private fun checkPermission() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        audioPermissions.launch(permissions)
    }

    // ================= ON CREATE =================

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        binding = ActivityAghoraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkPermission()

        onBackPressedDispatcher.addCallback(this) {
            openDialog()
        }

        setupClicks()
    }

    private fun setupClicks() {

        binding.leaveBtn.setOnClickListener {
            openDialog()
        }

        binding.audioBtn.setOnClickListener {
            isMuted = !isMuted
            rtcEngine?.muteLocalAudioStream(isMuted)
        }

        binding.videoBtn.setOnClickListener {
            isVideoMuted = !isVideoMuted
            rtcEngine?.muteLocalVideoStream(isVideoMuted)
        }

        binding.ivFlipCamera.setOnClickListener {
            rtcEngine?.switchCamera()
        }
    }

    // ================= DIALOG =================

    private fun openDialog() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.end_call_title))
            .setMessage(getString(R.string.end_call_message))
            .setPositiveButton(getString(R.string.yes)) { dialog, _ ->

                if (isConnectedToInternet(this, true)
                    && jitsiClass?.isClass == false
                ) {
                    userRepository.callStatus(
                        jitsiClass?.id ?: "",
                        jitsiClass?.call_id ?: "",
                        PushType.CALL_CANCELED
                    )
                }

                leaveChannel()
                finish()
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    // ================= INITIALISE =================

    private fun initialiseAgora() {

        LocaleHelper.setLocale(
            this,
            userRepository.getUserLanguage(),
            prefsManager
        )

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
        )

        jitsiClass = intent.getSerializableExtra(EXTRA_CALL_NAME) as JitsiClass

        rtcEngine = RtcEngine.create(baseContext, aghoraAppId, rtcEventHandler)
        rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

//        val channelName: String =
//            if (jitsiClass?.isClass == false) {
//                "Call_${appClientDetails.jitsi_id}_${jitsiClass?.id}"
//            } else {
//                "Class_${appClientDetails.jitsi_id}_${jitsiClass?.id}"
//            }

        val channelName = jitsiClass?.id

        val isAudioOnly =
            jitsiClass?.callType?.lowercase() == ConsultType.AUDIO_CALL ||
                    jitsiClass?.callType?.lowercase() == ConsultType.CALL

        if (isAudioOnly) {
            rtcEngine?.disableVideo()
        } else {
            rtcEngine?.enableVideo()
            setupLocalVideo()
        }

        Log.e("TAG", "initialiseAgora: ${jitsiClass?.id}" )

        rtcEngine?.joinChannel(
            jitsiClass?.agora_token,
            channelName,
            "",
            0
        )

        SoundPoolManager.getInstance(this)?.stopRinging()
    }

    // ================= RTC EVENTS =================

    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {
            Log.d(TAG, "Joined Channel")
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread {
                setupRemoteVideo(uid)
            }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread {
                leaveChannel()
                finish()
            }
        }

        override fun onLeaveChannel(stats: RtcStats?) {
            Log.d(TAG, "Channel left")
        }
    }

    // ================= VIDEO SETUP =================

    private fun setupLocalVideo() {
        val surfaceView = SurfaceView(baseContext)
        rtcEngine?.setupLocalVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )
        setContentView(surfaceView)
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = SurfaceView(baseContext)
        rtcEngine?.setupRemoteVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
        setContentView(surfaceView)
    }

    // ================= LEAVE =================

    private fun leaveChannel() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        rtcEngine = null
    }

    // ================= BROADCAST =================

    private val callCancelledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(EXTRA_REQUEST_ID) == jitsiClass?.call_id) {
                if (intent.action == com.consultantvendor.ui.calling.Constants.ACTION_CANCEL_CALL
                    || intent.action == PushType.REQUEST_COMPLETED
                ) {
                    leaveChannel()
                    finish()
                }
            }
        }
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(com.consultantvendor.ui.calling.Constants.ACTION_INCOMING_CALL)
            intentFilter.addAction(com.consultantvendor.ui.calling.Constants.ACTION_CANCEL_CALL)
            intentFilter.addAction(PushType.REQUEST_COMPLETED)

            LocalBroadcastManager.getInstance(this)
                .registerReceiver(callCancelledReceiver, intentFilter)

            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(callCancelledReceiver)
            isReceiverRegistered = false
        }
    }

    // ================= LIFECYCLE =================

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onPause() {
        super.onPause()
    }

    override fun onDestroy() {
        unregisterReceiver()
        leaveChannel()
        super.onDestroy()
    }
}