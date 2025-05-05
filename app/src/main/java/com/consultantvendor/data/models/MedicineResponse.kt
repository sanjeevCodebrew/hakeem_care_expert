package com.consultantvendor.data.models

data class MedicineResponse(
    val errorCode: String,
    val messageAR: String,
    val messageEN: String,
    val pages: Int,
    val response: List<ResponseMedicine>,
    val status: Boolean,
    val total: Int
)

data class ResponseMedicine(
    val category: String,
    val description: String,
    val sfda: String,
    val sku: String
)