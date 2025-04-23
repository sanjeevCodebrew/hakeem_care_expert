package com.consultantvendor.data.models.responses

data class LoggedInUser(
    val userId: String,
    val country_code: String,
    val moh: String,
    val username: String,
    val profileImageUrl: String = ""
)