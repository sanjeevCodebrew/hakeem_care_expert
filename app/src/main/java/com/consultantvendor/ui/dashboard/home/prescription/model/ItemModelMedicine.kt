package com.consultantvendor.ui.dashboard.home.prescription.model

import java.io.Serializable

data class ItemModelMedicine(
    var description: String? = null,
    var doses: String? = null,
    var frequency: String? = null,
    var duration: String? = null,
    var item_no: String? = null,
    var quantity: String? = null
) : Serializable
