package com.consultantvendor.data.models.responses

// Root of POST /api/zid/checkout
data class ZidCheckoutResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val checkout_url: String? = null
)
