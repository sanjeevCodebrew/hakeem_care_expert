package com.consultantvendor.ui.dashboard.home.prescription.model

data class InsuranceResponse(
    val errorCode: String,
    val messageAR: String,
    val messageEN: String,
    val response: List<ResponseInsurance>,
    val status: Boolean
)

data class ResponseInsurance(
    val _id: String,
    val code: String,
    val license: String,
    val titleAR: String,
    val titleEN: String
)