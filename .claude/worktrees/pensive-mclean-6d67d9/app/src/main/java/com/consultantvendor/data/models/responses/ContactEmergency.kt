package com.consultantvendor.data.models.responses

import java.io.Serializable

data class ContactEmergency(
        var id: String? = null,
        var contacts: ArrayList<ContactEmergency>? = null,

        /*Contacts*/
        var name: String? = null,
        var phone_numbers: ArrayList<ContactEmergency>? = null,

        /*Phone number*/
        var phone: String? = null,
        var type_label: String? = null

) : Serializable