package com.consultantvendor.data.models.responses

data class PreWrittenPrescriptionListData(
    val current_page: Int = 1,
    val data: List<PreWrittenPrescription> = emptyList()
)
