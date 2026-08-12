package com.consultantvendor.ui.dashboard.home

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.requests.DoctorNotesRequest
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.Extra_payment
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class AppointmentViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val pendingRequest by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val requestDetail by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val callStatus by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val acceptRequest by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val startRequest by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val startCall by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val completeChat by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val cancelRequest by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val extraPayment by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val updateCarePlan by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val publishPrescription by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val saveVitalSigns by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val saveDoctorNotes by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    fun request(hashMap: HashMap<String, String>) {
        pendingRequest.value = Resource.loading()

        webService.request(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            pendingRequest.value = Resource.success(response.body()?.data)
                        } else {
                            pendingRequest.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        pendingRequest.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun requestDetail(hashMap: HashMap<String, String>) {
        requestDetail.value = Resource.loading()

        webService.requestDetail(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            requestDetail.value = Resource.success(response.body()?.data)
                        } else {
                            requestDetail.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        requestDetail.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun callStatus(hashMap: HashMap<String, Any>) {
        callStatus.value = Resource.loading()

        webService.callStatus(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            callStatus.value = Resource.success(response.body()?.data)
                        } else {
                            callStatus.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        callStatus.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun acceptRequest(hashMap: HashMap<String, Any>) {
        acceptRequest.value = Resource.loading()

        webService.acceptRequest(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            acceptRequest.value = Resource.success(response.body()?.data)
                        } else {
                            acceptRequest.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        acceptRequest.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun acceptRequestV2(hashMap: HashMap<String, Any>) {
        acceptRequest.value = Resource.loading()

        webService.acceptRequestV2(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            acceptRequest.value = Resource.success(response.body()?.data)
                        } else {
                            acceptRequest.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        acceptRequest.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun startRequest(hashMap: HashMap<String, Any>) {
        startRequest.value = Resource.loading()

        webService.startRequest(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        startRequest.value = Resource.success(response.body()?.data)
                    } else {
                        startRequest.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    startRequest.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

    fun startCall(hashMap: HashMap<String, Any>) {
        startCall.value = Resource.loading()

        webService.startCall(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            startCall.value = Resource.success(response.body()?.data)
                        } else {
                            startCall.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        startCall.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun completeChat(hashMap: HashMap<String, Any>) {
        completeChat.value = Resource.loading()

        webService.completeChat(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            completeChat.value = Resource.success(response.body()?.data)
                        } else {
                            completeChat.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        completeChat.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun cancelRequest(hashMap: HashMap<String, String>) {
        cancelRequest.value = Resource.loading()

        webService.cancelRequest(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        cancelRequest.value = Resource.success(response.body()?.data)
                    } else {
                        cancelRequest.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    cancelRequest.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

    fun cancelRequestV2(hashMap: HashMap<String, Any>) {
        cancelRequest.value = Resource.loading()

        webService.cancelRequestV2(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            cancelRequest.value = Resource.success(response.body()?.data)
                        } else {
                            cancelRequest.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        cancelRequest.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun extraPayment(extraPaymentModel: Extra_payment) {
        extraPayment.value = Resource.loading()

        webService.extraPayment(extraPaymentModel)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            extraPayment.value = Resource.success(response.body()?.data)
                        } else {
                            extraPayment.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        extraPayment.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun updateCarePlan(hashMap: HashMap<String, Any>) {
        updateCarePlan.value = Resource.loading()

        webService.updateCarePlans(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            updateCarePlan.value = Resource.success(response.body()?.data)
                        } else {
                            updateCarePlan.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        updateCarePlan.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun saveVitalSigns(hashMap: HashMap<String, String>) {
        saveVitalSigns.value = Resource.loading()
        webService.saveVitalSigns(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {
                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>, response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        saveVitalSigns.value = Resource.success(response.body()?.data)
                    } else {
                        saveVitalSigns.value = Resource.error(ApiUtils.getError(response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    saveVitalSigns.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }

    fun saveDoctorNotes(request: DoctorNotesRequest) {
        saveDoctorNotes.value = Resource.loading()
        webService.saveDoctorNotes(request)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {
                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>, response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        saveDoctorNotes.value = Resource.success(response.body()?.data)
                    } else {
                        saveDoctorNotes.value = Resource.error(ApiUtils.getError(response.code(), response.errorBody()?.string()))
                    }
                }
                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    saveDoctorNotes.value = Resource.error(ApiUtils.failure(throwable))
                }
            })
    }

    fun publishPrescription(hashMap: HashMap<String, String>) {
        publishPrescription.value = Resource.loading()

        webService.publishPrescription(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        publishPrescription.value = Resource.success(response.body()?.data)
                    } else {
                        publishPrescription.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    publishPrescription.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }
}