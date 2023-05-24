package com.consultantvendor.data.models.responses

import java.io.Serializable

data class Page(
        var id: Int? = null,
        var icon: Int? = null,

        var slug: String? = null,
        var title: String? = null,
        var app_type: String? = null,

        var desc: String? = null,

        var question: String? = null,
        var answer: String? = null,

        var status: String? = null

) : Serializable