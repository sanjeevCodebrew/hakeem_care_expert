package com.consultantvendor.data.models.responses

import java.io.Serializable


data class Filter (
        var id: Int? = null,
        var category_id: Int? = null,
        var filter_name: String? = null,
        var preference_name: String? = null,
        var is_multi: String? = null,
        var options: List<FilterOption>? = null,

        /*Options*/
        var option_name: String? = null,
        var filter_type_id: Int? = null,
        var isSelected :Boolean= false,

        /*Symptom*/
        var name: String? = null,

        var title: String? = null,
        var tier_options: List<Filter>? = null,
        var type: String? = null,
        var status: String? = null
) :Serializable