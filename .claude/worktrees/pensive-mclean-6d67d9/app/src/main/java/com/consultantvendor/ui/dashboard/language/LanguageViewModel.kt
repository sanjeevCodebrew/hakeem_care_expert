package com.consultantvendor.ui.dashboard.language

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class LanguageViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val postLanguage by lazy { SingleLiveEvent<com.consultantvendor.data.network.responseUtil.Resource<CommonDataModel>>() }

    fun postLanguage(hashMap: HashMap<String, String>) {
        postLanguage.value = com.consultantvendor.data.network.responseUtil.Resource.loading()

        webService.postLanguage(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>
                ) {
                    if (response.isSuccessful) {
                        postLanguage.value = com.consultantvendor.data.network.responseUtil.Resource.success(response.body()?.data)
                    } else {
                        postLanguage.value = com.consultantvendor.data.network.responseUtil.Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    postLanguage.value = com.consultantvendor.data.network.responseUtil.Resource.error(
                        ApiUtils.failure(throwable))
                }

            })
    }

}