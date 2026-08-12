package com.consultantvendor.ui.chat

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.chat.chatMediaData
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ChatViewModel @Inject constructor(private val webService: WebService): ViewModel() {

    val chatListing by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val chatMessages by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val notifications by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val chatMedia by lazy { SingleLiveEvent<Resource<chatMediaData>>() }



    fun getChatListing(hashMap: HashMap<String, String>) {
        chatListing.value = Resource.loading()

        webService.getChatListing(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        chatListing.value = Resource.success(response.body()?.data)
                    } else {
                        chatListing.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    chatListing.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }


    fun getChatMessage(hashmap: HashMap<String, String>) {
        chatMessages.value = Resource.loading()
        webService.getChatMessage(hashmap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {
                    override fun onFailure(
                        call: Call<ApiResponse<CommonDataModel>>,
                        throwable: Throwable) {
                        chatMessages.value = Resource.error(ApiUtils.failure(throwable))
                    }

                    override fun onResponse(
                        call: Call<ApiResponse<CommonDataModel>>,
                        response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            chatMessages.value = Resource.success(response.body()?.data)
                        } else {
                            chatMessages.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }
                })
    }

    fun getChatMedia(hashMap: HashMap<String, String>) {
        chatMedia.value = Resource.loading()

        webService.getChatMedia(hashMap)
            .enqueue(object : Callback<ApiResponse<chatMediaData>> {
                override fun onResponse(
                    call: Call<ApiResponse<chatMediaData>>,
                    response: Response<ApiResponse<chatMediaData>>
                ) {
                    if (response.isSuccessful) {
                        chatMedia.value = Resource.success(response.body()?.data)
                    } else {
                        chatMedia.value = Resource.error(
                            ApiUtils.getError(response.code(), response.errorBody()?.string())
                        )
                    }
                }

                override fun onFailure(call: Call<ApiResponse<chatMediaData>>, throwable: Throwable) {
                    chatMedia.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }

    fun notifications(hashMap: HashMap<String, String>) {
        notifications.value = Resource.loading()

        webService.notifications(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        notifications.value = Resource.success(response.body()?.data)
                    } else {
                        notifications.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    notifications.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

}