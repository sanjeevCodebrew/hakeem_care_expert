package com.consultantvendor.data.models.requests

class SetService {
    var id: String? = null
    var available = "1"
    var price: Double? = null
    var minimmum_heads_up = "5"
    var isAvailabilityChanged = false
    var availability: SetAvailability? = null

    var clinic_address: SaveAddress? = null
}