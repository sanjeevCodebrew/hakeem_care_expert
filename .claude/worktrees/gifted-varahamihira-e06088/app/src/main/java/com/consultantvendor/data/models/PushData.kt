package com.consultantvendor.data.models

import java.io.Serializable

data class PushData(
    val msg: String?=null,
    var title: String?=null,
    val sound: String?=null,
    val pushType: String?=null,
    val imageUrl: String?=null,
    val message: String?=null,
    val senderId: String?=null,
    val senderName: String?=null,
    val receiverId: String?=null,
    val messageType: String?=null,
    val request_id: String?=null,
    val call_id: String?=null,
    val service_type: String?=null,
    val main_service_type:String?=null,
    val sentAt: Long?=null,
    val request_time: String?=null,
    val sender_name: String?=null,
    val sender_image: String?=null,
    val vendor_category_name: String?=null,
    val mohNumber: String?=null,
    val agora_token: String?=null
) : Serializable