package com.consultantvendor.data.models.requests

import java.io.Serializable

data class Interval (
    var start_time: String? = null,
    var end_time: String? = null
):Serializable