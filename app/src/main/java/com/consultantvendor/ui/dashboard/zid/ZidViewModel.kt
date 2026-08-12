package com.consultantvendor.ui.dashboard.zid

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.ZidCheckoutRequest
import com.consultantvendor.data.models.responses.ZidCheckoutResponse
import com.consultantvendor.data.models.responses.ZidProductsResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ZidViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val zidProducts by lazy { SingleLiveEvent<Resource<ZidProductsResponse>>() }
    val zidCheckout by lazy { SingleLiveEvent<Resource<ZidCheckoutResponse>>() }

    fun checkout(request: ZidCheckoutRequest) {
        zidCheckout.value = Resource.loading()
        webService.zidCheckout(request)
            .enqueue(object : Callback<ZidCheckoutResponse> {
                override fun onResponse(call: Call<ZidCheckoutResponse>, response: Response<ZidCheckoutResponse>) {
                    if (response.isSuccessful) zidCheckout.value = Resource.success(response.body())
                    else zidCheckout.value = Resource.error(ApiUtils.getError(response.code(), response.errorBody()?.string()))
                }
                override fun onFailure(call: Call<ZidCheckoutResponse>, throwable: Throwable) {
                    zidCheckout.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }

    fun getZidProducts(page: Int, search: String = "") {
        zidProducts.value = Resource.loading()
        val params = hashMapOf("page" to page.toString(), "page_size" to "10")
        // NOTE: search param excluded — Zid search API pending; filtering done locally in ZidFragment
        webService.getZidProducts(params)
            .enqueue(object : Callback<ZidProductsResponse> {
                override fun onResponse(call: Call<ZidProductsResponse>, response: Response<ZidProductsResponse>) {
                    if (response.isSuccessful) zidProducts.value = Resource.success(response.body())
                    else zidProducts.value = Resource.error(ApiUtils.getError(response.code(), response.errorBody()?.string()))
                }

                override fun onFailure(call: Call<ZidProductsResponse>, throwable: Throwable) {
                    zidProducts.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }
}
