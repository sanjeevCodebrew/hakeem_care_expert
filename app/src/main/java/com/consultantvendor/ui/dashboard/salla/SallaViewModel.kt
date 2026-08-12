package com.consultantvendor.ui.dashboard.salla

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.SallaCheckoutRequest
import com.consultantvendor.data.models.responses.SallaProductsResponse
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class SallaViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val sallaProducts by lazy { SingleLiveEvent<Resource<SallaProductsResponse>>() }
    val sallaCheckout by lazy { SingleLiveEvent<Resource<String>>() }

    fun checkout(request: SallaCheckoutRequest) {
        sallaCheckout.value = Resource.loading()
        webService.sallaCheckout(request)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {
                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>, response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) sallaCheckout.value = Resource.success(response.body()?.message)
                    else sallaCheckout.value = Resource.error(ApiUtils.getError(response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    sallaCheckout.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }

    fun getSallaProducts(page: Int, search: String = "") {
        sallaProducts.value = Resource.loading()
        val params = hashMapOf("page" to page.toString())
        if (search.isNotBlank()) params["search"] = search.trim()
        webService.getSallaProducts(params)
            .enqueue(object : Callback<SallaProductsResponse> {
                override fun onResponse(call: Call<SallaProductsResponse>, response: Response<SallaProductsResponse>) {
                    if (response.isSuccessful) sallaProducts.value = Resource.success(response.body())
                    else sallaProducts.value = Resource.error(ApiUtils.getError(response.code(), response.errorBody()?.string()))
                }

                override fun onFailure(call: Call<SallaProductsResponse>, throwable: Throwable) {
                    sallaProducts.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }
}
