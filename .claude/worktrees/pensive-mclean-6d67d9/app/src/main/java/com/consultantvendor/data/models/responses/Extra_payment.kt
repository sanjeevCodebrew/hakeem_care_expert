package com.consultantvendor.data.models.responses

import java.io.Serializable

class Extra_payment :Serializable{
    var id: Int? = null
    var request_id: String? = null
    var balance: String? = null
    var description: String? = null
    var status: String? = null
}