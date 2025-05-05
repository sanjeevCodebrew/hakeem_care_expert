package com.consultantvendor.data.models.responses

data class DiagnosisResponse(
    val errorCode: String,
    val messageAR: String,
    val messageEN: String,
    val pages: Int,
    val response: ArrayList<Response>,
    val status: Boolean,
    val total: Int
)

data class Response(
    val code: String,
    val title: String,
    val duration: String,
    val doses_type: String
)