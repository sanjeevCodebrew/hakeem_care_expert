package com.consultantvendor.ui.dashboard.home.questions

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

class QuestionViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val getQuestions by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val getQuestionsDetails by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val replyQuestion by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val getMedicalHistory by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val createMedicalHistory by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }



    fun getQuestions(hashMap: HashMap<String, String>) {
        getQuestions.value = Resource.loading()

        webService.getQuestions(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            getQuestions.value = Resource.success(response.body()?.data)
                        } else {
                            getQuestions.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        getQuestions.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun getQuestionsDetails(hashMap: HashMap<String, String>) {
        getQuestionsDetails.value = Resource.loading()

        webService.getQuestionsDetails(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            getQuestionsDetails.value = Resource.success(response.body()?.data)
                        } else {
                            getQuestionsDetails.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        getQuestionsDetails.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun replyQuestion(hashMap: HashMap<String, Any>) {
        replyQuestion.value = Resource.loading()

        webService.replyQuestion(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            replyQuestion.value = Resource.success(response.body()?.data)
                        } else {
                            replyQuestion.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        replyQuestion.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }


    fun createMedicalHistory(hashMap: HashMap<String, Any>) {
        createMedicalHistory.value = Resource.loading()

        webService.createMedicalHistory(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            createMedicalHistory.value = Resource.success(response.body()?.data)
                        } else {
                            createMedicalHistory.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        createMedicalHistory.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun getMedicalHistory(hashMap: HashMap<String, String>) {
        getMedicalHistory.value = Resource.loading()

        webService.getMedicalHistory(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            getMedicalHistory.value = Resource.success(response.body()?.data)
                        } else {
                            getMedicalHistory.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                        getMedicalHistory.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }


}