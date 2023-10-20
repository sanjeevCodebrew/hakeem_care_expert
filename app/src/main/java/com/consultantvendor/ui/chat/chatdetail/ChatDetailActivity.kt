package com.consultantvendor.ui.chat.chatdetail

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.BuildConfig
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.DocImage
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.chat.ChatMessage
import com.consultantvendor.data.network.*
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityChatDetailBinding
import com.consultantvendor.ui.chat.ChatViewModel
import com.consultantvendor.ui.chat.UploadFileViewModel
import com.consultantvendor.ui.dashboard.home.AppointmentViewModel
import com.consultantvendor.ui.dashboard.success.NetworkIssueFragment
import com.consultantvendor.utils.*
import com.consultantvendor.utils.AppSocket.Events.*
import com.consultantvendor.utils.AppSocket.Events.Companion.BROADCAST
import com.consultantvendor.utils.AppSocket.Events.Companion.DELIVERED_MESSAGE
import com.consultantvendor.utils.AppSocket.Events.Companion.READ_MESSAGE
import com.consultantvendor.utils.AppSocket.Events.Companion.SEND_MESSAGE
import com.consultantvendor.utils.AppSocket.Events.Companion.TYPING
import com.consultantvendor.utils.AppSocket.MessageStatus.Companion.DELIVERED
import com.consultantvendor.utils.AppSocket.MessageStatus.Companion.NOT_SENT
import com.consultantvendor.utils.AppSocket.MessageStatus.Companion.SEEN
import com.consultantvendor.utils.AppSocket.MessageStatus.Companion.SENT
import com.consultantvendor.utils.PermissionUtils
import com.consultantvendor.utils.dialogs.FileUriUtils
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.dialogs.ProgressDialogImage
import com.devlomi.record_view.OnRecordListener
import com.google.gson.Gson
import com.yanzhenjie.album.Album
import dagger.android.support.DaggerAppCompatActivity
import droidninja.filepicker.FilePickerConst
import droidninja.filepicker.utils.ContentUriUtils
import io.socket.client.Ack
import io.socket.emitter.Emitter
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.internal.filterList
import org.json.JSONException
import org.json.JSONObject
import permissions.dispatcher.*
import timber.log.Timber
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.concurrent.fixedRateTimer
import kotlin.concurrent.schedule


@RuntimePermissions
class ChatDetailActivity : DaggerAppCompatActivity(), AppSocket.OnMessageReceiver {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var appSocket: AppSocket

    @Inject
    lateinit var userRepository: UserRepository


    companion object {
        const val DELAY: Long = 2000
        var otherUserID = "-1"
        var requestId = "-1"
        var isActive = false
    }

    lateinit var binding: ActivityChatDetailBinding

    private lateinit var progressDialog: ProgressDialog

    private lateinit var progressDialogImage: ProgressDialogImage

    private lateinit var adapter: ChatDetailAdapter

    private lateinit var viewModel: ChatViewModel

    private lateinit var viewModelCall: AppointmentViewModel

    private var userID = ""

    private var userName = ""

    private var hasMoreItems = false

    private lateinit var llm: LinearLayoutManager

    private var items = ArrayList<ChatMessage>()

    private lateinit var viewModelUpload: UploadFileViewModel

    private var isTyping = false

    private var timer = Timer()

    private var timerTyping = Timer()

    private var countDownTimer: CountDownTimer? = null

    private val TOTAL_TIME = 60000L

    private var isReceiverRegistered = false

    private var audioFileName = ""

    private var timerPendingMessage = Timer()

    private var pageBeforeAfter: String? = null

    private var isLoadingItems = false

    private var lan  = ""

    var fileToUpload1 : File?=null



    var isStopRight = false
    var isStopLeft = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //LocaleHelper.setLocale(this, getUserLanguage())
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_detail)

        setAdapter()
        initialise()
        liveData()

        listeners()
        setButtonMicSend()
        checkNotSentMessage()
    }

    /*override fun attachBaseContext(base: Context?) {
        val locale = Locale(getUserLanguage())
        val contxt = ContextWrapper.wrap(base,locale)
        super.attachBaseContext(contxt)
    }*/

    private fun initialise() {
        audioFileName = "${externalCacheDir?.absolutePath}/${System.currentTimeMillis()}_audio.wav"
        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)

        progressDialog = ProgressDialog(this)
        progressDialogImage = ProgressDialogImage(this)
        viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]
        viewModelCall = ViewModelProvider(this, viewModelFactory)[AppointmentViewModel::class.java]
        viewModelUpload = ViewModelProvider(this, viewModelFactory)[UploadFileViewModel::class.java]
        registerReceiver(broadcastReceiver, IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION))

        requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""

        userID = userRepository.getUser()?.id ?: ""
        userName = intent.getStringExtra(USER_NAME) ?: ""
        binding.tvUserName.text = userName
        lan = userRepository.getUserLanguage()

    }


    override fun onStart() {
        super.onStart()

        socketEvents(makeOn = true)
    }

    private fun socketEvents(makeOn: Boolean = true) {
        appSocket.off(TYPING, listener)
        appSocket.off(READ_MESSAGE, listenerRead)
        appSocket.off(DELIVERED_MESSAGE, listenerDelivered)
        appSocket.off(BROADCAST, listenerStatus)
        appSocket.removeOnMessageReceiver(this)

        if (makeOn) {
            if (!appSocket.isConnected)
                appSocket.init()

            appSocket.addOnMessageReceiver(this)
            appSocket.on(TYPING, listener)
            appSocket.on(READ_MESSAGE, listenerRead)
            appSocket.on(DELIVERED_MESSAGE, listenerDelivered)
            appSocket.on(BROADCAST, listenerStatus)
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        socketEvents(makeOn = false)
        unregisterReceiver(broadcastReceiver)
        unregisterReceiver()
        timerPendingMessage.cancel()
    }

    override fun onBackPressed() {
        if (items.isNotEmpty()) {
            val intent = Intent()
            intent.putExtra(LAST_MESSAGE, Gson().toJson(items[0]))
            intent.putExtra(OTHER_USER_ID, otherUserID)
            intent.putExtra(EXTRA_REQUEST_ID, requestId)
            setResult(Activity.RESULT_OK, intent)
        }
        otherUserID = "-1"
        super.onBackPressed()
    }

    private fun getChatData() {
        if (isConnectedToInternet(this, true)) {
            val hashMap = HashMap<String, String>()

            when {
                items.isNotEmpty() && pageBeforeAfter == ApiKeys.AFTER -> {
                    hashMap[ApiKeys.AFTER] = items[items.size - 1].messageId.toString()
                    hashMap[ApiKeys.PER_PAGE] = PER_PAGE_LOAD_CHAT.toString()
                }
                items.isNotEmpty() && pageBeforeAfter == ApiKeys.BEFORE -> {
                    hashMap[ApiKeys.BEFORE] = items[0].messageId.toString()
                    hashMap[ApiKeys.PER_PAGE] = "1000"
                }
                else -> hashMap[ApiKeys.PER_PAGE] = PER_PAGE_LOAD_CHAT.toString()
            }

            hashMap["request_id"] = requestId

            if (!isLoadingItems) {
                isLoadingItems = true
                viewModel.getChatMessage(hashMap)
            }
        }
    }

    private fun distinctList(messagesNew: List<ChatMessage>?) {
        val tempList = ArrayList<ChatMessage>()
        when {
            pageBeforeAfter.isNullOrEmpty() -> {
                /*If message added from socket*/
                if (items.isNotEmpty())
                    tempList.addAll(items)
                tempList.addAll(messagesNew ?: emptyList())

                /*If message is not read*/
                if (items.isNotEmpty())
                    sendMessageRead(items[0].messageId)

            }
            pageBeforeAfter == ApiKeys.AFTER -> {
                tempList.addAll(items)
                tempList.addAll(messagesNew ?: emptyList())
            }
            pageBeforeAfter == ApiKeys.BEFORE -> {
                tempList.addAll(messagesNew ?: emptyList())
                tempList.addAll(items)
            }
        }

        items.clear()
        items.addAll(tempList.distinctBy { it.messageId })
        adapter.notifyDataSetChanged()

    /*val tempList = ArrayList<ChatMessage>()
        tempList.addAll(items.distinctBy { it.messageId })
        items.clear()
        items.addAll(tempList)

        if (size == 0) {
            adapter.notifyDataSetChanged()
        } else {
            adapter.notifyItemRangeInserted(size, items.size)
            adapter.notifyItemRangeChanged(size - 1, items.size)
        }*/
    }

    private fun liveData() {
        viewModel.chatMessages.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    isLoadingItems = false
                    val data = it.data

                    /*User status*/
                    setStatus(data?.isOnline)
                    /*Has more data*/
                    hasMoreItems = if (pageBeforeAfter.isNullOrEmpty() || pageBeforeAfter == ApiKeys.AFTER)
                        data?.messages?.size == PER_PAGE_LOAD_CHAT
                    else true
                    /*Check for distinct list*/
                    distinctList(data?.messages)
                    /*Send chat start message*/
                    if (intent.hasExtra(EXTRA_IS_FIRST)) {
                        generateNewMessage(getString(R.string.chat_first_message, userName))
                        intent.removeExtra(EXTRA_IS_FIRST)
                    }
                    /*Show Timer if needed*/
                    showTimer(data?.request_status == CallAction.INPROGRESS, data)
                    /*Hide Loader*/
                    binding.clLoader.gone()
                    binding.clLoader.setBackgroundResource(0)
                    binding.pbLoaderBottom.gone()
                }

                Status.ERROR -> {
                    isLoadingItems = false
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    if (pageBeforeAfter.isNullOrEmpty() || pageBeforeAfter == ApiKeys.BEFORE) {
                        if (items.isEmpty())
                            binding.clLoader.setBackgroundResource(R.color.colorWhite)
                        binding.clLoader.visible()
                    } else
                        binding.pbLoaderBottom.visible()
                }
            }
        })

        viewModelUpload.uploadFile.observe(this, Observer {
            resources ?: return@Observer

            when (it.status) {

                Status.SUCCESS -> {
                    progressDialogImage.setLoading(false)
                    val docImage = DocImage()
                    docImage.type = it.data?.type
                    docImage.image = it.data?.image_name
                    if (isConnectedToInternet(this, false)) {
                        sendImage(docImage)
                    } else {
                        binding.etMessage.showSnackBar(getString(R.string.check_internet))
                    }
                }
                Status.ERROR -> {
                    progressDialogImage.setLoading(false)
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    progressDialogImage.setLoading(true)
                }
            }
        })

        viewModelCall.completeChat.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    showTimer(false, null)

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })
    }

    /*Show time and complete button*/
    private fun showTimer(show: Boolean, data: CommonDataModel?) {
        if (show) {
            binding.tvCompleteChat.visible()
            binding.rlChatInput.visible()

            binding.tvTimer.visible()
            startTimer(((data?.currentTimer ?: 0) * 1000))
        } else {
            binding.tvCompleteChat.gone()
            binding.rlChatInput.gone()

            binding.tvTimer.gone()
            countDownTimer?.cancel()
            binding.tvCompleteChat.hideKeyboard()
        }

    }

    private fun startTimer(currentTimer: Long) {
        countDownTimer?.cancel()

        val totalTimerToRun = (TOTAL_TIME * 1000) + currentTimer
        countDownTimer = object : CountDownTimer(totalTimerToRun, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val newTimer = ((totalTimerToRun - millisUntilFinished) + currentTimer) / 1000
                binding.tvTimer.text = convertMiliSecondsToMinute(newTimer)
            }

            override fun onFinish() {}
        }
        countDownTimer?.start()
    }


    private fun convertMiliSecondsToMinute(seconds: Long): String {
        val s = seconds % 60
        val m = seconds / 60 % 60
        val h = seconds / 3600
        return String.format("%s : %02d : %02d", h.toString(), m, s)
    }

    private fun setStatus(isOnline: Boolean?) {
        runOnUiThread {
            if (isOnline == true)
                binding.tvUserStatus.text = getString(R.string.active_now)
            else
                binding.tvUserStatus.text = ""
        }
    }

    private fun sendImage(docImage: DocImage) {

        val msg = ChatMessage(
                imageUrl = docImage.image,
                message = "",
                senderId = userID,
                senderName = userRepository.getUser()?.name,
                receiverId = otherUserID,
                messageType = docImage.type,
                request_id = requestId,
                sentAt = System.currentTimeMillis(),
                status = NOT_SENT)
        sendMessage(msg)
    }


    private fun listeners() {
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                Log.e("TextChanged", "afterTextChanged")
            }

            override fun beforeTextChanged(s: CharSequence, start: Int,
                                           count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                Log.e("TextChanged", "onTextChanged")
                setButtonMicSend()

                timer = Timer()
                if (!isTyping) {
                    isTyping = true
                    startTyping()
                }
                timer.cancel()
                timer = Timer()
                timer.schedule(object : TimerTask() {
                    override fun run() {
                        isTyping = false
                        stopTypingSocket()
                    }
                }, DELAY)
            }
        })

        /*Start Recording*/
        binding.recordButton.setRecordView(binding.recordView)
        binding.recordButton.isListenForRecord = checkIfPermission()
        binding.recordView.setSoundEnabled(false)
        binding.recordView.setLessThanSecondAllowed(false)


//            if (lan=="ar") {
//                Log.e("TAG", "chkLan: "+lan)
//                binding.recordView.layoutDirection = View.LAYOUT_DIRECTION_LTR
//                binding.recordButton.layoutDirection = View.LAYOUT_DIRECTION_LTR
//                binding.rlChatInput.layoutDirection = View.LAYOUT_DIRECTION_LTR
//            }

        binding.recordButton.setOnLongClickListener {
            if (checkIfPermission()) {
                binding.recordButton.isListenForRecord = true
                binding.llChat.invisible()
                binding.recordButton.setImageDrawable(getDrawable(R.drawable.ic_mic))
                true
            } else {
                binding.recordButton.isListenForRecord = false
                getAudioWithPermissionCheck()
                false
            }
        }

        binding.recordView.setOnRecordListener(object : OnRecordListener {
            @SuppressLint("LogNotTimber")
            override fun onFinish(recordTime: Long) {
                binding.llChat.visible()

                setButtonMicSend()
                stopRecording()

                if (File(audioFileName).exists()) {
                    val docImage = DocImage()
                    docImage.imageFile = File(audioFileName)
                    docImage.type = DocType.AUDIO
                    uploadFileOnServer(docImage)
                }
                Log.e("RECORDER", "onFinish")
            }

            override fun onLessThanSecond() {
                binding.llChat.visible()
                stopRecording()
                setButtonMicSend()
                Log.e("RECORDER", "Less than one second")

            }

            override fun onCancel() {
                stopRecording()
                binding.llChat.visible()
                Log.e("RECORDER", "On Cancel")

            }

            override fun onStart() {
                if (checkIfPermission()) {

                    binding.llChat.invisible()
                    startRecording()
                }
                Log.e("RECORDER", "On Start")
            }

        })

        binding.recordView.setOnBasketAnimationEndListener {
            binding.llChat.visible()
            setButtonMicSend()
        }


        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        binding.btnCamera.setOnClickListener {
            binding.btnCamera.hideKeyboard()
            getStorageWithPermissionCheck()

        }

        binding.ivSend.setOnClickListener {
            if (binding.etMessage.text.toString().trim().isEmpty()) {
                binding.etMessage.error = getString(R.string.enter_message)
                return@setOnClickListener
            }

            generateNewMessage(binding.etMessage.text.toString().trim())
        }

        binding.tvCompleteChat.setOnClickListener {
            showCompleteRequestDialog()
        }

        binding.rvChatData.addOnScrollListener(onScrollListener)
    }

    private fun setButtonMicSend() {
        runOnUiThread {
            if (binding.etMessage.text.toString().trim().isEmpty()) {
                binding.ivSend.gone()
                binding.recordButton.visible()
            } else {
                binding.recordButton.gone()
                binding.ivSend.visible()
            }
        }
    }

    private fun generateNewMessage(message: String) {
        if (isConnectedToInternet(this, false)) {

            val msg = ChatMessage(
                    imageUrl = String(),
                    message = message,
                    senderId = userID,
                    senderName = userRepository.getUser()?.name,
                    receiverId = otherUserID,
                    messageType = DocType.TEXT,
                    request_id = requestId,
                    sentAt = System.currentTimeMillis(),
                    status = NOT_SENT)

            sendMessage(msg)
        } else {
            binding.etMessage.showSnackBar(getString(R.string.check_internet))
        }

    }

    private var onScrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = binding.rvChatData.layoutManager as LinearLayoutManager
            val totalItemCount = layoutManager.itemCount - 1
            val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

            if (hasMoreItems && lastVisibleItemPosition >= totalItemCount) {
                pageBeforeAfter = ApiKeys.AFTER
                getChatData()
            }
        }
    }

    private fun sendMessageRead(id: String?) {
        val obj = JSONObject()
        obj.put("messageId", id)
        obj.put("receiverId", otherUserID)
        obj.put("senderId", userID)
        appSocket.emit(READ_MESSAGE, obj, Ack {

        })
    }

    /*--------   TYPING ----------*/
    private fun startTyping() {
        val obj = JSONObject()
        obj.put("isTyping", true.toString())
        obj.put("receiverId", otherUserID)
        obj.put("senderId", userID)
        appSocket.emit(TYPING, obj)
    }

    private fun stopTypingSocket() {
        val obj = JSONObject()
        obj.put("isTyping", false.toString())
        obj.put("receiverId", otherUserID)
        obj.put("senderId", userID)
        appSocket.emit(TYPING, obj)
    }

    private val listener = Emitter.Listener {
        Timber.e("Typing $it")
        val senderId = (it[0] as JSONObject).getString("senderId")
        val isTyping = (it[0] as JSONObject).getBoolean("isTyping")
        if (senderId == otherUserID) {
            if (isTyping) {
                runOnUiThread {
                    binding.tvUserStatus.visible()
                    binding.tvUserStatus.text = getString(R.string.typing)
                }
                timerTyping.cancel()
                timerTyping.purge()
                timerTyping = Timer()
                timerTyping.schedule(4000) {
                    binding.tvUserStatus.invisible()
                    runOnUiThread { binding.tvUserStatus.text = getString(R.string.active_now) }
                }
            } /*else {
                runOnUiThread {
                    binding.tvUserStatus.text = getString(R.string.active_now)
                }
            }*/
        }
    }

    private val listenerRead = Emitter.Listener {
        Timber.e("Read $it")

        val data = it[0] as JSONObject
        val messageId = data.getString("messageId")

        runOnUiThread {
            items.forEachIndexed { index, chatMessage ->
                if (items[index].status == SENT || items[index].status == DELIVERED) {
                    items[index].status = SEEN

                    adapter.notifyItemChanged(items.indexOf(chatMessage))
                }
            }
        }
    }

    private val listenerDelivered = Emitter.Listener {
        Timber.e("Delivered $it")

        val data = it[0] as JSONObject
        val messageId = data.getString("messageId")

        runOnUiThread {
            items.forEachIndexed { index, chatMessage ->
                if (items[index].status == SENT) {
                    items[index].status = DELIVERED

                    adapter.notifyItemChanged(items.indexOf(chatMessage))
                }
            }
        }
    }

    private val listenerStatus = Emitter.Listener {
        Timber.e("BroadCast $it")
        val senderId = (it[0] as JSONObject).getString("userId")
        val isOnline = (it[0] as JSONObject).getBoolean("isOnline")
        if (senderId == otherUserID)
            setStatus(isOnline)
    }


    private fun checkNotSentMessage() {
        timerPendingMessage.cancel()

        val timeDelay = 15000L
        timerPendingMessage = fixedRateTimer("timerMessage", true, timeDelay, timeDelay) {
            if (isConnectedToInternet(this@ChatDetailActivity, false))
                sendNotSentMessage()
        }
    }

    private fun sendNotSentMessage() {
        val currentTime = System.currentTimeMillis()

        val messagesNotSent = items.filterList {
            this.status == NOT_SENT && this.senderId == userID && (currentTime - (this.sentAt ?: currentTime) > 8000)
        }

        Log.e("messagesNotSent", Gson().toJson(messagesNotSent).toString())

        messagesNotSent.forEach {
            sendMessage(it, true)
        }
    }


    private fun sendMessage(chatMessage: ChatMessage, notSentMessage: Boolean = false) {
        if (appSocket.isConnected && isConnectedToInternet(this@ChatDetailActivity, false)) {

            /*Add Message to list*/
            if (!notSentMessage) {
                binding.etMessage.setText("")
                setButtonMicSend()

                items.add(0, chatMessage)
                adapter.notifyItemInserted(0)
                binding.rvChatData.scrollToPosition(0)
            }

            /*Send event*/
            try {
                val jsonObject = JSONObject(Gson().toJson(chatMessage))

                appSocket.emit(SEND_MESSAGE, jsonObject, Ack {
                    val data = it[0] as JSONObject
                    Log.e("ack**", data.toString())

                    /*If request completed end chat*/
                    if (data.optString("status") == PushType.REQUEST_COMPLETED) {
                        runOnUiThread {
                            showTimer(false, null)
                            longToast(getString(R.string.request_completed))
                        }
                    } else if (!data.optString("messageId").isNullOrEmpty()) {
                        runOnUiThread {
                            Log.e("ack========", data.optString("messageId"))

                            val indexOfMessage = items.indexOf(chatMessage)
                            if (items[indexOfMessage].status == NOT_SENT) {
                                items[indexOfMessage].status = SENT
                                if (items[indexOfMessage].messageId.isNullOrEmpty())
                                    items[indexOfMessage].messageId = data.optString("messageId")
                                adapter.notifyItemChanged(indexOfMessage)
                            }

                            /*items.forEachIndexed { index, chatMessage ->
                                if (chatMessage.status == AppSocket.MessageStatus.NOT_SENT) {
                                    *//*If message is not read*//*
                                    items[index].status = AppSocket.MessageStatus.SENT
                                    adapter.notifyItemChanged(items.indexOf(chatMessage))
                                }
                            }*/
                        }
                    }

                })
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        } else {
            if (!notSentMessage && !isConnectedToInternet(this,false))
                longToast(getString(R.string.check_internet))
            socketEvents(makeOn = true)
        }

    }

    private fun setAdapter() {
        llm = LinearLayoutManager(this)
        llm.reverseLayout = true
        llm.orientation = LinearLayoutManager.VERTICAL
        binding.rvChatData.layoutManager = llm

        adapter = ChatDetailAdapter(this, items)
        binding.rvChatData.adapter = adapter
    }


    override fun onMessageReceive(message: ChatMessage?) {
        runOnUiThread {
            if (message?.senderId == otherUserID && message.request_id == requestId) {
                sendMessageRead(message.messageId)

                message.let { items.add(0, it) }

                adapter.notifyItemInserted(0)
                binding.rvChatData.scrollToPosition(0)
            }
        }
    }


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION == intent.action) {

            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.IMAGE_PICKER -> {
                    val docPaths = ArrayList<Uri>()
                    docPaths.addAll(data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_MEDIA)
                            ?: emptyList())

                    val fileToUpload = compressImage(this, File(ContentUriUtils.getFilePath(this, docPaths[0])))

                    val docImage = DocImage()
                    docImage.type = DocType.IMAGE
                    docImage.imageFile = fileToUpload

                    uploadFileOnServer(docImage)
                }

                AppRequestCode.CAMERA -> {
                    val bitmap = data?.extras?.get("data") as Bitmap
                    val tempUri: Uri? = getImageUri1(this@ChatDetailActivity, bitmap)
                    val fileToUpload = getRealPathFromURI(tempUri)
                        .let { File(it) }

                    val docImage = DocImage()
                    docImage.type = DocType.IMAGE
                    docImage.imageFile = fileToUpload

                    uploadFileOnServer(docImage)
                }

                AppRequestCode.DOC_PICKER -> {
               /*     val docPaths = ArrayList<Uri>()
                    docPaths.addAll(data?.getParcelableArrayListExtra(FilePickerConst.KEY_SELECTED_DOCS)
                            ?: emptyList())

                    val fileToUpload = File(ContentUriUtils.getFilePath(this, docPaths[0]))

                    val docImage = DocImage()
                    docImage.type = DocType.PDF
                    docImage.imageFile = fileToUpload

                    uploadFileOnServer(docImage)*/

                }
            }
        }
    }

    fun getImageUri1(inContext: Context, inImage: Bitmap): Uri? {
        val bytes = ByteArrayOutputStream()
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes)
        val path = MediaStore.Images.Media.insertImage(inContext.getContentResolver(), inImage, "IMG_" + Calendar.getInstance().getTime(),null)
        return Uri.parse(path)
    }

    fun getRealPathFromURI(uri: Uri?): String {
        var path = ""
        if (contentResolver != null) {
            val cursor = uri?.let { contentResolver!!.query(it, null, null, null, null) }
            if (cursor != null) {
                cursor.moveToFirst()
                val idx: Int = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA)
                path = cursor.getString(idx)
                cursor.close()
            }
        }
        return path
    }


    private fun uploadFileOnServer(docImage: DocImage?) {
        val hashMap = HashMap<String, RequestBody>()
        hashMap["type"] = getRequestBody(docImage?.type)

        val body: RequestBody = docImage?.imageFile?.asRequestBody("image/*".toMediaType())!!
        hashMap["image\"; fileName=\"" + docImage?.imageFile?.name] = body

        viewModelUpload.uploadFile(hashMap)
    }


    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<out String>,
            grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        onRequestPermissionsResult(requestCode, grantResults)
    }

    @NeedsPermission(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun getStorage() {
        askForOption1(null, this, binding.btnCamera)
    }

    @OnShowRationale(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showLocationRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(this, R.string.media_permission, request)
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(
                this, R.string.media_permission
        )
    }

    @OnPermissionDenied(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(
                this, R.string.media_permission
        )
    }

    @NeedsPermission(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun getAudio() {
        binding.recordButton.isListenForRecord = true
    }

    @OnShowRationale(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showAudioRationale(request: PermissionRequest) {
        PermissionUtils.showRationalDialog(this, R.string.record_audio_permission, request)
    }

    @OnNeverAskAgain(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun onAudioNeverAskAgainRationale() {
        PermissionUtils.showAppSettingsDialog(this, R.string.record_audio_permission)
    }

    @OnPermissionDenied(Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    fun showAudioDeniedForStorage() {
        PermissionUtils.showAppSettingsDialog(this, R.string.record_audio_permission)
    }


    private fun showCompleteRequestDialog() {
        AlertDialogUtil.instance.createOkCancelDialog(this, R.string.end_chat,
                R.string.end_chat_desc, R.string.end_chat, R.string.cancel, false,
                object : AlertDialogUtil.OnOkCancelDialogListener {
                    override fun onOkButtonClicked() {
                        hitApiAcceptRequest()
                    }

                    override fun onCancelButtonClicked() {
                    }
                }).show()
    }

    private fun hitApiAcceptRequest() {
        if (isConnectedToInternet(this, true)) {
            val hashMap = HashMap<String, Any>()
            hashMap["request_id"] = requestId

            viewModelCall.completeChat(hashMap)
        }
    }


    override fun onPause() {
        super.onPause()
        isActive = false
        otherUserID = "-1"
    }

    override fun onResume() {
        super.onResume()
        isActive = true
        registerReceiver()

        otherUserID = intent.getStringExtra(USER_ID) ?: ""

        pageBeforeAfter = if (pageBeforeAfter == null) "" else ApiKeys.BEFORE
        getChatData()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.REQUEST_COMPLETED)
            intentFilter.addAction(PushType.COMPLETED)
            intentFilter.addAction(NetworkIssueFragment.NETWORK_ISSUE)
            LocalBroadcastManager.getInstance(this).registerReceiver(refreshRequests, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(refreshRequests)
            isReceiverRegistered = false
        }
    }

    private val refreshRequests = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.REQUEST_COMPLETED, PushType.COMPLETED -> {
                    if (intent.getStringExtra(EXTRA_REQUEST_ID) == requestId) {
                        showTimer(false, null)
                        longToast(getString(R.string.request_completed))
                    }
                }
                NetworkIssueFragment.NETWORK_ISSUE -> {
                    pageBeforeAfter = if (pageBeforeAfter == null) "" else ApiKeys.BEFORE
                    getChatData()
                }
            }

        }
    }

    /*Audio Recoder*/
    private var recorder: MediaRecorder? = null
    private var player: MediaPlayer? = null

    private fun startRecording() {
        if (File(audioFileName).exists())
            File(audioFileName).delete()

        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(audioFileName)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)

            try {
                prepare()
            } catch (e: IOException) {
                Log.e("audioFileName", "prepare() failed")
            }

            start()
        }
    }

    private fun stopRecording() {
        try {
            recorder?.apply {
                stop()
                release()
            }
            recorder = null
        } catch (ignored: RuntimeException) {
        }
    }

    fun startPlaying(link: String) {
        /* val fragment = BottomAudioPlayerFragment(link)
         fragment.show(supportFragmentManager, fragment.tag)*/

//        val viewMediaIntent = Intent()
//        viewMediaIntent.action = Intent.ACTION_VIEW
//        viewMediaIntent.setDataAndType(Uri.parse(link), "audio/*")
//        viewMediaIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
//
//        if (this.packageName.equals(BuildConfig.APPLICATION_ID))
//            startActivity(viewMediaIntent)

        player = MediaPlayer().apply {
            try {
                setDataSource(this@ChatDetailActivity, Uri.parse(link))
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("MediaPlayer", "prepare() failed")
            }
        }


        player!!.setOnCompletionListener(MediaPlayer.OnCompletionListener {

            isStopRight=true
            adapter.notifyDataSetChanged()
            Log.e("TAG", "completeAudio: "+player )
        })



    }

    fun startPlaying1(link: String) {

        player = MediaPlayer().apply {
            try {
                setDataSource(this@ChatDetailActivity, Uri.parse(link))
                prepare()
                start()
            } catch (e: IOException) {
                Log.e("MediaPlayer", "prepare() failed")
            }
        }


        player!!.setOnCompletionListener(MediaPlayer.OnCompletionListener {

            isStopLeft=true
            adapter.notifyDataSetChanged()
            Log.e("TAG", "completeAudio: "+player )
        })
    }

    private fun stopPlaying() {
        player?.release()
        player = null

    }

    private fun checkIfPermission(): Boolean {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED)
    }

    fun askForOption1(fragment: Fragment?, activity: Activity, view: View) {
        val context: Context = fragment?.requireContext() ?: activity

        val popup = PopupMenu(context, view)
        popup.menuInflater.inflate(R.menu.menu_attach, popup.menu)

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.item_image_camera-> {
                    openCamera(activity,fragment)
                }
                R.id.item_image-> {
                    selectImages(fragment, activity)
                }
                R.id.item_pdf -> {
                    selectDocument2()
                }
            }
            true
        }

        popup.show()
    }

/*    private fun openAlbum1(activity: Activity, fragment: Fragment?) {

        Album.camera(activity) // Camera function.
            .image() // Take Picture.
            .onResult {
                val path = it
//                val intent :Intent? = Intent()
//              intent?.putExtra("path", path.toUri())


//                val fileToUpload =
//                    compressImage(this,
//                        ContentUriUtils.getFilePath(this, path.toUri())?.let { it1 -> File(it1) })
                val fileToUpload = path?.let { File(it) }

                val docImage = DocImage()
                docImage.type = DocType.IMAGE
                docImage.imageFile = fileToUpload
                uploadFileOnServer(docImage)

            }
            .onCancel { }
            .start()

    }*/

    private fun selectDocument2() {
        val mimeType = "application/pdf"
        /*Single Document Picker*/
        // Image , Video , PDF , DOC , DOCX
        pickMedia.launch(
            arrayOf(mimeType)
        )

        /* Multiple Document Picker*/
        pickMultipleDocument.launch(arrayOf(mimeType))
    }

    val pickMedia = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            Log.e("PhotoPicker", "Selected URI: $uri")

            val file = FileUriUtils.getRealPath(this, uri)?.let { File(it) }
            Log.e("file", "" + file?.exists())
            Log.e("fileLength", "" + file?.length())
            Log.e("fileName", "" + file?.name)
            Log.e("filePath", "" + file?.path)
            Log.e("file.extension", "" + file?.extension)

            if (file != null) {

                if ((file.extension.equals("pdf", true)) ||
                    (file.extension.equals("doc", true)) ||
                    (file.extension.equals("docx", true)) ||
                    (file.extension.equals("mp4", true)) ||
                    (file.extension.equals("mp3", true)) ||
                    (file.extension.equals("eac3", true)) ||
                    (file.extension.equals("wav", true)) ||
                    (file.extension.equals("mov", true)) ||
                    (file.extension.equals("avi", true)) ||
                    (file.extension.equals("mkv", true)) ||
                    (file.extension.equals("webm", true))
                ) {

                    /*   img_pick.setImageBitmap(
                           FileUtil.getThumbnail(
                               file,
                               uri,
                               context = applicationContext
                           )
                       )*/

                } else if ((file.extension.equals("jpg", true)) ||
                    (file.extension.equals("jpeg", true)) ||
                    (file.extension.equals("png", true))
                ) {

//                    img_pick.setImageURI(uri)

                }
            } else {
//                img_pick.setImageResource(R.drawable.img_not_supported)
                Toast.makeText(this, "This file format is not supported", Toast.LENGTH_SHORT).show()
            }


        } else {
            Log.d("PhotoPicker", "No media selected")
        }
    }


    val pickMultipleDocument =
        registerForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
            if (uris.isNotEmpty()){

                for (i in uris.indices){
                    val file = FileUriUtils.getRealPath(this, uris[i])?.let { File(it) }
                    Log.e("file", "" + file?.exists())
                    Log.e("fileLength", "" + file?.length())
                    Log.e("fileName", "" + file?.name)
                    Log.e("filePath", "" + file?.path)
                    Log.e("file.extension", "" + file?.extension)


                    fileToUpload1 = file
                    Log.e("TAG", "checkDoc: "+fileToUpload1?.toURI())

                }
                val docImage = DocImage()
                docImage.type = DocType.PDF
                docImage.imageFile = fileToUpload1
                uploadFileOnServer(docImage)
            }

        }
}