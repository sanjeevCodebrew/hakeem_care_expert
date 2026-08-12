package com.consultantvendor.data.models

data class DrugListResponse(
    val status: String,
    val statuscode: Int,
    val message: String,
    val data: DrugListData
)

data class DrugListData(
    val current_page: Int,
    val data: List<DrugItem>
)

data class DrugItem(
    val id: Int,
    val category1: String?,
    val code: String?,
    val display: String?,
    val dosage_form: String?,
    val ingredients: String?,
    val strength: String?
)
