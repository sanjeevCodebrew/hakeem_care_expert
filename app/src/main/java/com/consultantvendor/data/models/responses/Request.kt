package com.consultantvendor.data.models.responses

import com.consultantvendor.data.models.requests.AddPrescription
import com.consultantvendor.data.models.requests.DocImage
import java.io.Serializable

class Request : Serializable {

    var id: String? = null
    var booking_date: String? = null
    var from_user: UserData? = null
    var to_user: UserData? = null
    var time: String? = null
    var service_type: String ? = null
    var insurance_name: String ? = null
    var insurance_number: String ? = null
    var main_service_type: String? = null
    var parent_cat_name: String? = null
    var status: String? = null
    var price: String? = null
    var created_at: String? = null
    var bookingDateUTC: String? = null
    var booking_end_date: String? = null
    var canReschedule = false
    var canCancel = false

    var call_id: String? = null
    var is_prescription: Boolean? = null
    var is_report: Boolean? = null
    var is_prescription_report: Boolean? = null
    var extra_detail: Extra_detail? = null

    var symptoms: List<Filter>? = null
//    var medical_reports: List<MedicalReport>? = null
    var symptom_details: String? = null
    var symptom_images: List<DocImage>? = null

    var pre_scription: AddPrescription? = null

    var extra_payment: Extra_payment? = null

    var cancel_reason: String? = null

    var care_plans: List<Page>? = null
    var question_answers: List<Page>? = null
    var tier_detail: Filter? = null
    var medical_history_added:Boolean?=null
    var remain_second:Long?=null
    // Added categoryData
    var categoryData: CategoryData? = null

    var medicalReport: MedicalReport?=null
}

class CategoryData : Serializable {
    var id: Int? = null
    var name: String? = null
    var ar_name: String? = null
    var image: String? = null
    var parent_id: Int? = null
    var payment_type: String? = null
    var description: String? = null
    var color_code: String? = null
    var enable: Boolean? = null
    var cat_slug: String? = null
}

