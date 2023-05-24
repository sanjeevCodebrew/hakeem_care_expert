package com.consultantvendor.data.models.responses

import java.io.Serializable


data class MedicalHistory (
        var id: Int? = null,
        var request_id: Int? = null,
        var comment: String? = null,
        var preference_name: String? = null,
        var request: Request? = null,

) :Serializable