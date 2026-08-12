package com.consultantvendor.data.models.requests

import java.io.Serializable

class SaveAddress : Serializable {
    var locationName: String? = null
    var houseNumber: String? = null
    var isDefault = false
    var _id: String? = null
    var addressId: String? = null

    var lat: Double? = null
    var long: Double? = null
}