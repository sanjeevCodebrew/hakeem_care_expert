package com.consultantvendor.ui.aghora

import android.Manifest
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.JitsiClass
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityAghoraBinding
import com.consultantvendor.utils.ConsultType
import com.consultantvendor.utils.EXTRA_CALL_NAME
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.loadImage
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerAppCompatActivity
import io.agora.rtc2.ChannelMediaOptions
import io.agora.rtc2.Constants
import io.agora.rtc2.IRtcEngineEventHandler
import io.agora.rtc2.RtcEngine
import io.agora.rtc2.RtcEngineConfig
import io.agora.rtc2.video.VideoCanvas
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject


class AghoraNewActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository
    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: ActivityAghoraBinding
    private var rtcEngine: RtcEngine? = null
    private var jitsiClass: JitsiClass? = null

    private var isMuted = false
    private var speakerEnabled = false

    private lateinit var audioManager: AudioManager

    private val agoraAppId = "b25e3d53ae804174a0c341c3dcaa25cd"

    private var timerJob: Job? = null

    private var seconds = 0



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAghoraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager
        jitsiClass = intent.getSerializableExtra(EXTRA_CALL_NAME) as? JitsiClass

        binding.tvUsername.text = jitsiClass?.name
        loadImage(binding.ivPic, jitsiClass?.profileImage, R.drawable.ic_profile_placeholder)

        checkPermissions()
        listners()
    }


    private fun listners() {
        binding.ivFlipCamera.setOnClickListener {
            rtcEngine?.switchCamera()
        }
    }

    // ================= PERMISSION =================

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )

        val granted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) ==
                    PackageManager.PERMISSION_GRANTED
        }

        if (granted) initialiseAgora()
        else requestPermissions(permissions, 100)
    }

    // ================= INITIALISE =================

    private fun initialiseAgora() {

        val config = RtcEngineConfig().apply {
            mContext = applicationContext
            mAppId = agoraAppId
            mEventHandler = rtcEventHandler
        }

        rtcEngine = RtcEngine.create(config)
        rtcEngine?.setChannelProfile(Constants.CHANNEL_PROFILE_COMMUNICATION)
        rtcEngine?.setClientRole(Constants.CLIENT_ROLE_BROADCASTER)

        audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_VOICE_CALL,
            AudioManager.AUDIOFOCUS_GAIN
        )

        val channelName = jitsiClass?.id ?: return
        val token = "${jitsiClass?.agora_token}=="
        val uid = 0

       Log.e("TAG", "initialiseAgora: channelName:${channelName} token : ${token}")

        val isAudioOnly =
            jitsiClass?.callType?.lowercase() == ConsultType.AUDIO_CALL ||
                    jitsiClass?.callType?.lowercase() == ConsultType.CALL

        if (isAudioOnly) {
            setupAudioUI()
            startCallTimer(false)
            rtcEngine?.disableVideo()
        } else {
            setupVideoUI()
            rtcEngine?.enableVideo()
            setupLocalVideo()
        }

        val options = ChannelMediaOptions().apply {
            clientRoleType = Constants.CLIENT_ROLE_BROADCASTER
            channelProfile = Constants.CHANNEL_PROFILE_COMMUNICATION
            publishCameraTrack = !isAudioOnly
            publishMicrophoneTrack = true
        }

        rtcEngine?.joinChannel(token, channelName, uid, options)
    }

    // ================= UI SETUP =================

    private fun setupAudioUI() {

        binding.clAudioCall.visibility = View.VISIBLE
        binding.bgVideoContainer.visibility = View.GONE
        binding.floatingVideoContainer.visibility = View.GONE

        binding.audioBtn.visibility = View.VISIBLE
        binding.leaveBtn.visibility = View.VISIBLE
        binding.speakerButton.visibility = View.VISIBLE

        binding.ivFlipCamera.visibility = View.GONE
        binding.ivFlipCamera.visibility = View.GONE
        binding.videoBtn.visibility = View.GONE
        binding.ivSwitchVideoCall.visibility = View.GONE
        binding.ivBTSpeaker.visibility = View.GONE
    }

    private fun setupVideoUI() {

        binding.clAudioCall.visibility = View.GONE

        binding.bgVideoContainer.visibility = View.VISIBLE
        binding.floatingVideoContainer.visibility = View.VISIBLE

        binding.audioBtn.visibility = View.VISIBLE
        binding.leaveBtn.visibility = View.VISIBLE

        binding.ivFlipCamera.visibility = View.VISIBLE

        binding.speakerButton.visibility = View.VISIBLE
        binding.videoBtn.visibility = View.GONE
        binding.ivSwitchVideoCall.visibility = View.GONE
        binding.ivBTSpeaker.visibility = View.GONE
    }

    // ================= RTC EVENTS =================

    private val rtcEventHandler = object : IRtcEngineEventHandler() {

        override fun onJoinChannelSuccess(channel: String?, uid: Int, elapsed: Int) {

            val isAudioOnly =
                jitsiClass?.callType?.lowercase() == ConsultType.AUDIO_CALL ||
                        jitsiClass?.callType?.lowercase() == ConsultType.CALL

            audioManager.mode = AudioManager.MODE_IN_COMMUNICATION

            if (isAudioOnly) {
                speakerEnabled = false
                rtcEngine?.setEnableSpeakerphone(false)
                audioManager.isSpeakerphoneOn = false
                binding.speakerButton.setImageResource(R.drawable.ic_speaker_disable)
            } else {
                speakerEnabled = true
                rtcEngine?.setEnableSpeakerphone(true)
                audioManager.isSpeakerphoneOn = true
                binding.speakerButton.setImageResource(R.drawable.ic_speaker)
            }
        }

        override fun onUserJoined(uid: Int, elapsed: Int) {
            runOnUiThread { setupRemoteVideo(uid) }
        }

        override fun onUserOffline(uid: Int, reason: Int) {
            runOnUiThread { leaveCall() }
        }
    }

    // ================= VIDEO =================

    private fun setupLocalVideo() {

        rtcEngine?.startPreview()

        binding.llVc.visible()
        binding.tvVideoUserVC.text = jitsiClass?.name
        startCallTimer(true)

        val textureView = TextureView(this)

        binding.floatingVideoContainer.removeAllViews()
        binding.floatingVideoContainer.addView(textureView)

        rtcEngine?.setupLocalVideo(
            VideoCanvas(textureView, VideoCanvas.RENDER_MODE_HIDDEN, 0)
        )
    }

    private fun setupRemoteVideo(uid: Int) {
        val surfaceView = SurfaceView(this)
        binding.bgVideoContainer.removeAllViews()
        binding.bgVideoContainer.addView(surfaceView)

        rtcEngine?.setupRemoteVideo(
            VideoCanvas(surfaceView, VideoCanvas.RENDER_MODE_HIDDEN, uid)
        )
    }

    // ================= BUTTON CALLBACKS =================

    fun onAudioMuteClicked(view: View) {
        isMuted = !isMuted
        rtcEngine?.muteLocalAudioStream(isMuted)

        binding.audioBtn.setImageResource(
            if (isMuted) R.drawable.mute
            else R.drawable.unmute
        )
    }

    fun onSpeakerEnabledClick(view: View) {
        speakerEnabled = !speakerEnabled
        rtcEngine?.setEnableSpeakerphone(speakerEnabled)
        audioManager.isSpeakerphoneOn = speakerEnabled

        binding.speakerButton.setImageResource(
            if (speakerEnabled)
                R.drawable.ic_speaker
            else
                R.drawable.ic_speaker_disable
        )
    }

    fun onLeaveChannelClicked(view: View) {
        leaveCall()
    }

    private fun leaveCall() {
        stopCallTimer()
        userRepository.callStatus(
            jitsiClass?.id ?: "",
            jitsiClass?.call_id ?: "",
            PushType.CALL_CANCELED
        )

        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        finish()
    }

    override fun onDestroy() {
        rtcEngine?.leaveChannel()
        RtcEngine.destroy()
        super.onDestroy()
    }

    private fun startCallTimer(isFromVideo: Boolean) {

        timerJob = lifecycleScope.launch {

            while (isActive) {

                delay(1000)

                seconds++

                val minutes = seconds / 60
                val sec = seconds % 60

                val time = String.format("%02d:%02d", minutes, sec)

                if (isFromVideo) {
                    binding.tvTimerVC.text = time
                } else {
                    binding.tvTimer.text = time
                }
            }
        }
    }

    private fun stopCallTimer() {
        timerJob?.cancel()
    }
}