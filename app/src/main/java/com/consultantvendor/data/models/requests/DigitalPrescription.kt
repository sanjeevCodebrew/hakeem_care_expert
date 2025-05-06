package com.consultantvendor.data.models.requests

import java.io.Serializable

class DigitalPrescription : Serializable {
    var medicine_name: String? = null
    var duration: String? = null
    var quantity: String? = null
    var dosage_timing: ArrayList<Doases>? = null
    var dosage_type: String? = null
    var doses: String? = null
}