package com.consultantvendor.ui.dashboard.home

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class HomeViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val home by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }
    val banners by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }
    val notificationCount by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }
    val getProfile by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }
    val postLanguage by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }


    fun banners() {
        banners.value = Resource.loading()

        webService.banners().enqueue(object : Callback<ApiResponse<CommonDataModel>> {

            override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                    response: Response<ApiResponse<CommonDataModel>>) {
                if (response.isSuccessful) {
                    banners.value = Resource.success(response.body()?.data)
                } else {
                    banners.value = Resource.error(
                        ApiUtils.getError(response.code(),
                            response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                banners.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }


    fun home() {
        home.value = Resource.loading()

        webService.home()
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            home.value = Resource.success(response.body()?.data)
                        } else {
                            home.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        home.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun notificationCount() {
        notificationCount.value = Resource.loading()

        webService.notificationCount()
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        notificationCount.value = Resource.success(response.body()?.data)
                    } else {
                        notificationCount.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    notificationCount.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

    fun getprofile1() {
        getProfile.value = Resource.loading()
        webService.getProfile()
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        getProfile.value = Resource.success(response.body()?.data)
                    } else {
                        getProfile.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    getProfile.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

    fun postLanguage1(hashMap: HashMap<String, String>) {
        postLanguage.value = Resource.loading()

        webService.postLanguage(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        postLanguage.value = Resource.success(response.body()?.data)
                    } else {
                        postLanguage.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    postLanguage.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }
}