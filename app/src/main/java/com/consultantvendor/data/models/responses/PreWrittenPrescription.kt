package com.consultantvendor.data.models.responses

import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelDiagnosis
import com.consultantvendor.ui.dashboard.home.prescription.model.ItemModelMedicine
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PreWrittenPrescription(
    val id: Int? = null,
    val title: String? = null,
    val description: String? = null,
    val prescription_type: String? = null,
    val fill_type: String? = null,
    val report_detals: String? = null,
    val prescription_file: String? = null,
    @SerializedName("diagnoses")
    val diagnosis: List<ItemModelDiagnosis>? = null,
    @SerializedName("medicines")
    val prescription: List<ItemModelMedicine>? = null
) : Serializable
