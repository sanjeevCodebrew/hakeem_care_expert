package com.consultantvendor.data.models.responses.chat

data class chatMediaData(
    val media: List<ChatMedia>
)

data class ChatMedia(
    val id: Int,
    val image_url: String,
    val message_type: String,
    val request_id: Int,
    val receiver_id: Int,
    val user_id: Int,
    val status: String
)
