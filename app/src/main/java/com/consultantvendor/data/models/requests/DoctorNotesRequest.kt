package com.consultantvendor.data.models.requests

data class DoctorNotesRequest(
    val request_id: String,
    val chiefComplain: String,
    val briefHistory: String,
    val diagnoses: List<DiagnosisItem>,
    val plan: String
)

data class DiagnosisItem(
    val code: String,
    val title: String
)
