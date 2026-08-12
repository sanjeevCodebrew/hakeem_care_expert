package com.consultantvendor.data.models.requests

import java.io.Serializable

class SetFilter :Serializable{
    var filter_id: Int? = null
    var filter_option_ids: ArrayList<String>? = null

    var preference_id: Int? = null
    var option_ids: ArrayList<String>? = null
}