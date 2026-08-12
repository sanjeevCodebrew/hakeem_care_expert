package com.consultantvendor.ui.dashboard.settings.contactlist

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.ContactEmergency
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class ContactViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val contactList by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val sendMessage by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val addContact by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val deletContact by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    fun contactList(hashMap: HashMap<String, String>) {
        contactList.value = Resource.loading()

        webService.contactList(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            contactList.value = Resource.success(response.body()?.data)
                        } else {
                            contactList.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        contactList.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun addContact(contactList: ContactEmergency) {
        addContact.value = Resource.loading()

        webService.addContact(contactList)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            addContact.value = Resource.success(response.body()?.data)
                        } else {
                            addContact.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        addContact.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun sendMessage() {
        sendMessage.value = Resource.loading()

        webService.sendMessage()
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        sendMessage.value = Resource.success(response.body()?.data)
                    } else {
                        sendMessage.value = Resource.error(ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    sendMessage.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

    fun deletContact(hashMap: HashMap<String, Any>) {
        deletContact.value = Resource.loading()

        webService.deletContact(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            deletContact.value = Resource.success(response.body()?.data)
                        } else {
                            deletContact.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        deletContact.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }
}