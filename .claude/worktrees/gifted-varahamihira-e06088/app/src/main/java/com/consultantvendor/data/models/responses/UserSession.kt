package com.consultantvendor.data.models.responses

data class UserSession(
    val userId: String,
    val moh: String,
    val token: String,
    val username: String,
    var isSelect: Boolean,
    val profileImageUrl: String = ""
)