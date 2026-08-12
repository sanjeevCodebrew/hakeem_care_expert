package com.consultantvendor.data.models.responses

import java.io.Serializable

class WaterIntake : Serializable {
    var limit: Double? = null
    var total_achieved_goal: String? = null
    var today_intake: Double? = null
}