package com.consultantvendor.data.models.responses

import java.io.Serializable

class MedicalReport : Serializable {
    val created_at: String?=null
    val diagnosis: List<Diagnosis>?=null
    val fill_type: String?=null
    val id: Int?=null
    val insurance_id: String?=null
    val prescription: List<Prescription>?=null
    val prescription_file: String?=null
    val prescription_type: String?=null
    val report_detals: String?=null
    val request_id: Int?=null
    val updated_at: String?=null
}

data class Diagnosis(
    val code: String,
    val created_at: String,
    val id: Int,
    val medical_report_id: Int,
    val title: String,
    val updated_at: String
) : Serializable

data class Prescription(
    val created_at: String,
    val description: String,
    val doses: String,
    val duration: String,
    val frequency: String,
    val id: Int,
    val item_no: String,
    val medical_report_id: Int,
    val quantity: String,
    val updated_at: String
) : Serializable