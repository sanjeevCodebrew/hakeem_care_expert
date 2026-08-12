package com.consultantvendor.data.models.responses

data class SallaProductsResponse(
    val status: Int? = null,
    val success: Boolean? = null,
    val data: List<SallaProduct>? = null,
    val pagination: SallaPagination? = null
)

data class SallaPagination(
    val count: Int? = null,
    val total: Int? = null,
    @com.google.gson.annotations.SerializedName("per_page")
    val perPage: Int? = null,
    @com.google.gson.annotations.SerializedName("current_page")
    val currentPage: Int? = null,
    @com.google.gson.annotations.SerializedName("total_pages")
    val totalPages: Int? = null
)
