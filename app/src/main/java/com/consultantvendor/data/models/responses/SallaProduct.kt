package com.consultantvendor.data.models.responses

import java.io.Serializable

data class SallaProduct(
    val id: Long? = null,
    val name: String? = null,
    val thumbnail: String? = null,
    val main_image: String? = null,
    val price: SallaPrice? = null,
    val sale_price: SallaPrice? = null,
    val regular_price: SallaPrice? = null,
    val status: String? = null,
    val description: String? = null,
    val quantity: Int? = null,
    val sku: String? = null,
    val options: List<SallaProductOption>? = null
) : Serializable

// Option group from the product listing API (e.g. "Flavor" with values ["Strawberry", "Chocolate"])
data class SallaProductOption(
    val id: Long? = null,
    val name: String? = null,
    val display_type: String? = null,
    val values: List<SallaOptionValue>? = null
) : Serializable

data class SallaOptionValue(
    val id: Long? = null,
    val name: String? = null
) : Serializable

data class SallaPrice(
    val amount: Double? = null,
    val currency: String? = null
) : Serializable

// Checkout payload models
data class SallaCheckoutRequest(
    val request_id: String? = null,
    val products: List<SallaCheckoutItem>
)

data class SallaCheckoutItem(
    val identifier_type: String = "id",
    val identifier: Long,
    val quantity: Int = 1,
    val options: List<SallaCheckoutOption>? = null
)

// One entry per option group: id = option group id, value = selected value id (as string)
data class SallaCheckoutOption(
    val id: Long,
    val value: String
)
