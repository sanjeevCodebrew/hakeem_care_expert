package com.consultantvendor.data.models.responses

import java.io.Serializable

data class ZidProduct(
    val id: String? = null,
    val sku: String? = null,
    val name: ZidLocalizedText? = null,
    val price: Double? = null,
    val sale_price: Double? = null,
    val formatted_price: String? = null,
    val formatted_sale_price: String? = null,
    val currency: String? = null,
    val quantity: Int? = null,
    val is_infinite: Boolean? = null,
    val is_published: Boolean? = null,
    val has_options: Boolean? = null,
    val images: List<ZidProductImage>? = null
) : Serializable {

    val displayName: String get() = name?.en?.takeIf { it.isNotBlank() } ?: name?.ar ?: ""

    // In stock when quantity is unlimited or a positive quantity remains
    val inStock: Boolean get() = is_infinite == true || (quantity ?: 0) > 0
}

// Zid returns product names/descriptions localized by language code
data class ZidLocalizedText(
    val en: String? = null,
    val ar: String? = null
) : Serializable

data class ZidProductImage(
    val id: String? = null,
    val image: ZidImageUrls? = null
) : Serializable

data class ZidImageUrls(
    val thumbnail: String? = null,
    val small: String? = null,
    val medium: String? = null,
    val full_size: String? = null,
    val large: String? = null
) : Serializable

// Checkout payload models
data class ZidCheckoutRequest(
    val request_id: String? = null,
    val products: List<ZidCheckoutItem>
)

data class ZidCheckoutItem(
    val sku: String,
    val quantity: Int = 1
)
