package com.consultantvendor.data.models.responses

import com.consultantvendor.data.models.requests.SaveAddress
import com.consultantvendor.data.models.requests.SetAvailability
import java.io.Serializable

class Service :Serializable{
    var id: String? = null
    var category_id: String? = null
    var service_id: String? = null
    var is_active: String? = null
    var price_minimum: Double? = null
    var price_maximum: Double? = null
    var price_fixed: Double? = null
    var minimum_duration: Int? = null
    var gap_duration: Int? = null
    var created_at: String? = null
    var updated_at: String? = null
    var name: String? = null
    var description: String? = null
    var need_availability: String? = null
    var price_type: String? = null
    var unit_price: Double? = null

    var isSelected = false
    var setAvailability: SetAvailability? = null
    var clinic_address: SaveAddress? = null
    var isAvailabilityChanged = false
    var isAvailabilityLocal = false
    var price: String? = null
    var available:String?=null

    var sp_id: Int? = null
    var category_service_id: Int? = null
    var duration: String? = null
    var minimmum_heads_up: String? = null
    var deleted_at: Any? = null
    var category_name: String? = null
    var service_name: String? = null
    var main_service_type: String? = null
    var color_code: String? = null

    var count:String?=null
}