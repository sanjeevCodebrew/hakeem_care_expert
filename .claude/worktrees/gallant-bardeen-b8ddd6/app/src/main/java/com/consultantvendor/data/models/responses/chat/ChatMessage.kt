package com.consultantvendor.data.models.responses.chat

import com.consultantvendor.utils.MediaUploadStatus

data class ChatMessage(
        val imageUrl: String? = null,
        val message: String? = null,
        val senderId: String? = null,
        val senderName: String? = null,
        val receiverId: String? = null,
        var messageType: String? = null,
        val request_id: String? = null,
        val sentAt: Long? = null,
        var status: String? = null,

        var conversationId: String? = null,
        //val deleteByList: List<Any>? = null,
        var isActive: Boolean? = null,
        var createAt: String? = null,
        var __v: Int? = null,
        var mediaUploadStatus: String = MediaUploadStatus.UPLOADED,
        var id: String? = null,
        var messageId: String? = null
)