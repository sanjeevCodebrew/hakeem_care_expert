package com.consultantvendor.data.models.responses

// Root of GET /api/zid/products — Zid wraps the paginated results one level
// deeper than Salla does: {"status": true, "message": "...", "data": {...}}
data class ZidProductsResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: ZidProductsData? = null
)

data class ZidProductsData(
    val total_products_count: Int? = null,
    val count: Int? = null,
    val next: String? = null,
    val previous: String? = null,
    val results: List<ZidProduct>? = null
)
