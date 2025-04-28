package com.consultantvendor.data.models.responses

data class UserSession(
    val userId: String,
    val country_code: String,
    val moh: String,
    val token: String,
    val username: String,
    val profileImageUrl: String = ""
)