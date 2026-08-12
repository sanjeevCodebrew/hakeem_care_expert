package com.consultantvendor.data.models.responses

data class IcdDiagnosisListResponse(
    val status: String,
    val statuscode: Int,
    val message: String,
    val data: IcdDiagnosisListData
)

data class IcdDiagnosisListData(
    val current_page: Int,
    val data: List<IcdDiagnosisItem>
)

data class IcdDiagnosisItem(
    val id: Int,
    val code_id: String?,
    val ascii_desc: String?,
    val ascii_short_desc: String?
)
