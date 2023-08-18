package com.consultantvendor.utils

import android.os.Handler
import android.os.Looper
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.models.responses.chat.ChatMessage
import com.consultantvendor.data.repos.UserRepository
import com.google.gson.Gson
import io.socket.client.Ack
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.net.URISyntaxException
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by Rishi Sharma on 9/8/17.
 */
@Singleton
class AppSocket @Inject internal constructor(private val userRepository: UserRepository, gson: Gson?) {
    private var mSocket: Socket? = null
    private var manualReconnectTimer = Timer()
    private val onMessageReceiverList: MutableList<OnMessageReceiver> = ArrayList()
    private val onConnectionListeners: MutableList<ConnectionListener> = ArrayList()
    private val onConnect = Emitter.Listener { args: Array<Any?>? ->
        manualReconnectTimer.cancel()
        Timber.e("AppSocket - onConnect called")
        notifyConnectionListeners(Socket.EVENT_CONNECT)
    }
    private val onDisconnect = Emitter.Listener { args: Array<Any?>? ->
        Timber.e("AppSocket - onDisconnect called")
        restartManualReconnection()
        notifyConnectionListeners(Socket.EVENT_DISCONNECT)
    }
    private val onError = Emitter.Listener { args: Array<Any?>? ->
        Timber.e("AppSocket -onError called")
        restartManualReconnection()
        notifyConnectionListeners(Socket.EVENT_ERROR)
    }
    private val onTimeOut = Emitter.Listener { args: Array<Any?>? ->
        Timber.e("AppSocket -onTimeOut called")
        restartManualReconnection()
        notifyConnectionListeners(Socket.EVENT_CONNECT_TIMEOUT)
    }
    private val onReconnecting = Emitter.Listener { args: Array<Any?>? ->
        Timber.e("AppSocket -onReconnecting called")
        restartManualReconnection()
        notifyConnectionListeners(Socket.EVENT_RECONNECTING)
    }
    private val onReconnectError = Emitter.Listener { args: Array<Any?>? ->
        Timber.e("AppSocket -onReconnectError called")
        restartManualReconnection()
        notifyConnectionListeners(Socket.EVENT_RECONNECT_ERROR)
    }

    fun init(): Boolean {
        onMessageReceiverList.clear()
        onConnectionListeners.clear()
        return try {
            if (mSocket != null) {
                mSocket?.off()
                mSocket?.close()
            }
            if (userRepository.isUserLoggedIn()) {
                Timber.e("Socket${appClientDetails.socket_url.toString()}${userRepository.getUser()?.id}".trimIndent())
//                Timber.e("Socket${"https://socket.hakeemcare.com"}${userRepository.getUser()?.id}".trimIndent())
                val options = IO.Options()
                options.forceNew = false
                options.reconnection = true
                options.query = "user_id=" + userRepository.getUser()?.id + "&domain=" + userRepository.getAppSetting().domain
                mSocket = IO.socket(appClientDetails.socket_url?:"", options)
                connect()
                mSocket?.on(Socket.EVENT_CONNECT, onConnect)
                mSocket?.on(Socket.EVENT_DISCONNECT, onDisconnect)
                mSocket?.on(Socket.EVENT_CONNECT_ERROR, onError)
                mSocket?.on(Socket.EVENT_ERROR, onError)
                mSocket?.on(Socket.EVENT_CONNECT_TIMEOUT, onTimeOut)
                mSocket?.on(Socket.EVENT_RECONNECTING, onReconnecting)
                mSocket?.on(Socket.EVENT_RECONNECT_ERROR, onReconnectError)
                mSocket?.on(Socket.EVENT_RECONNECT_FAILED, onReconnectError)
                true
            } else {
                false
            }
        } catch (e: URISyntaxException) {
            e.printStackTrace()
            false
        }
    }

    val isConnected: Boolean
        get() = (mSocket?.connected()==true)

    private fun restartManualReconnection() {
        manualReconnectTimer.cancel()
        manualReconnectTimer = Timer()
        val MANUAL_RECONNECT_INTERVAL = 10000
        manualReconnectTimer.schedule(object : TimerTask() {
            override fun run() {
                mSocket?.io()?.reconnection(true)
                connect()
                Timber.e("AppSocket --> ManualReconnection Timer Task Called")
            }
        }, MANUAL_RECONNECT_INTERVAL.toLong())
    }

    val socket: Socket?
        get() {
            if (mSocket?.connected()==false) connect()
            return mSocket
        }

    fun connect() {
        if (mSocket?.connected()==false) mSocket?.connect()
    }

    fun disconnect() {
        mSocket?.disconnect()
    }

    fun emit(event: String?, vararg args: Any?) {
        mSocket?.emit(event, *args)
    }

    fun on(event: String?, fn: Emitter.Listener?) {
        mSocket?.on(event, fn)
    }

    fun off() {
        mSocket?.off()
    }

    fun off(event: String?) {
        mSocket?.off(event)
    }

    fun off(event: String?, fn: Emitter.Listener?) {
        mSocket?.off(event, fn)
    }

    fun sendMessage(message: ChatMessage?, msgAck: OnMessageReceiver) {
        var jsonObject: JSONObject? = null
        try {
            jsonObject = JSONObject(Gson().toJson(message))
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        mSocket?.emit(Events.SEND_MESSAGE, jsonObject, Ack { args: Array<Any> -> Handler(Looper.getMainLooper()).post { msgAck.onMessageReceive(Gson().fromJson(args[0].toString(), ChatMessage::class.java)) } })
    }

    fun sendMessageDelivery(id: String?, receiverID: String?) {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("messageId", id)
            jsonObject.put("receiverId", receiverID)
            mSocket?.emit(Events.DELIVERED_MESSAGE, jsonObject)
        } catch (e: JSONException) {
            e.printStackTrace()
        }
    }

    fun addConnectionListener(listener: ConnectionListener) {
        onConnectionListeners.add(listener)
    }

    fun removeConnectionListener(listener: ConnectionListener) {
        onConnectionListeners.remove(listener)
    }

    fun removeAllConnectionListeners() {
        onConnectionListeners.clear()
    }

    private fun notifyConnectionListeners(status: String) {
        for (listener in onConnectionListeners) {
            Handler(Looper.getMainLooper()).post { listener.onConnectionStatusChanged(status) }
        }
    }

    fun addOnMessageReceiver(receiver: OnMessageReceiver) {
        if (onMessageReceiverList.isEmpty()) {
            onReceiveMessageEvent()
        }
        onMessageReceiverList.add(receiver)
    }

    fun removeOnMessageReceiver(receiver: OnMessageReceiver) {
        onMessageReceiverList.remove(receiver)
        if (onMessageReceiverList.isEmpty()) {
            mSocket?.off(Events.RECEIVE_MESSAGE)
        }
    }

    fun removeAllMessageReceivers() {
        onMessageReceiverList.clear()
        mSocket?.off(Events.RECEIVE_MESSAGE)
    }

    private fun onReceiveMessageEvent() {
        mSocket?.on(Events.RECEIVE_MESSAGE) { args: Array<Any> ->
            val chat: ChatMessage
            chat = Gson().fromJson(args[0].toString(), ChatMessage::class.java)
            notifyMessageReceivers(chat)
        }
    }

    private fun notifyMessageReceivers(message: ChatMessage) {
        for (receiver in onMessageReceiverList) {
            Handler(Looper.getMainLooper()).post { receiver.onMessageReceive(message) }
        }
    }

    interface Events {
        companion object {
            const val SEND_MESSAGE = "sendMessage"
            const val RECEIVE_MESSAGE = "messageFromServer"
            const val TYPING = "typing"
            const val BROADCAST = "broadcast"
            const val ACKNOWLEDGE_MESSAGE = "acknowledgeMessage"
            const val READ_MESSAGE = "readMessage"
            const val DELIVERED_MESSAGE = "deliveredMessage"
            const val SEND_LIVE_LOCATION = "sendlivelocation"
        }
    }

    interface MessageStatus {
        companion object {
            const val NOT_SENT = "NOT_SENT"
            const val SENDING = "SENDING"
            const val SENT = "SENT"
            const val DELIVERED = "DELIVERED"
            const val SEEN = "SEEN"
        }
    }

    interface OnMessageReceiver {
        fun onMessageReceive(message: ChatMessage?)
    }

    interface ConnectionListener {
        fun onConnectionStatusChanged(status: String?)
    }

    init {
        init()
    }
}