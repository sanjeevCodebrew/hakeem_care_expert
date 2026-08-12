package com.consultantvendor.ui.dashboard.home.healthtool.waterintake

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.responses.WaterIntake
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class WaterIntakeViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val setDailyLimit by lazy { SingleLiveEvent<Resource<WaterIntake>>() }

    val getWaterIntake by lazy { SingleLiveEvent<Resource<WaterIntake>>() }

    val setWaterIntake by lazy { SingleLiveEvent<Resource<WaterIntake>>() }

    val setProteinDailyLimit by lazy { SingleLiveEvent<Resource<WaterIntake>>() }

    val getProteinIntake by lazy { SingleLiveEvent<Resource<WaterIntake>>() }

    val setProteinIntake by lazy { SingleLiveEvent<Resource<WaterIntake>>() }


    fun setDailyLimit(hashMap: HashMap<String,Any>) {
        setDailyLimit.value = Resource.loading()

        webService.setDailyLimit(hashMap).enqueue(object : Callback<ApiResponse<WaterIntake>> {

            override fun onResponse(call: Call<ApiResponse<WaterIntake>>,
                                    response: Response<ApiResponse<WaterIntake>>) {
                if (response.isSuccessful) {
                    setDailyLimit.value = Resource.success(response.body()?.data)
                } else {
                    setDailyLimit.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                    response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<WaterIntake>>, throwable: Throwable) {
                setDailyLimit.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }

    fun setWaterIntake(hashMap: HashMap<String,Any>) {
        setWaterIntake.value = Resource.loading()

        webService.setWaterIntake(hashMap).enqueue(object : Callback<ApiResponse<WaterIntake>> {

            override fun onResponse(call: Call<ApiResponse<WaterIntake>>,
                                    response: Response<ApiResponse<WaterIntake>>) {
                if (response.isSuccessful) {
                    setWaterIntake.value = Resource.success(response.body()?.data)
                } else {
                    setWaterIntake.value = Resource.error(
                        ApiUtils.getError(response.code(),
                            response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<WaterIntake>>, throwable: Throwable) {
                setWaterIntake.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }


    fun getWaterIntake(hashMap: HashMap<String,String>) {
        getWaterIntake.value = Resource.loading()

        webService.getWaterLimit(hashMap).enqueue(object : Callback<ApiResponse<WaterIntake>> {

            override fun onResponse(call: Call<ApiResponse<WaterIntake>>,
                                    response: Response<ApiResponse<WaterIntake>>) {
                if (response.isSuccessful) {
                    getWaterIntake.value = Resource.success(response.body()?.data)
                } else {
                    getWaterIntake.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                    response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<WaterIntake>>, throwable: Throwable) {
                getWaterIntake.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }

    fun setProteinDailyLimit(hashMap: HashMap<String,Any>) {
        setProteinDailyLimit.value = Resource.loading()

        webService.setProteinDailyLimit(hashMap).enqueue(object : Callback<ApiResponse<WaterIntake>> {

            override fun onResponse(call: Call<ApiResponse<WaterIntake>>,
                                    response: Response<ApiResponse<WaterIntake>>) {
                if (response.isSuccessful) {
                    setProteinDailyLimit.value = Resource.success(response.body()?.data)
                } else {
                    setProteinDailyLimit.value = Resource.error(
                        ApiUtils.getError(response.code(),
                            response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<WaterIntake>>, throwable: Throwable) {
                setProteinDailyLimit.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }

    fun setProteinIntake(hashMap: HashMap<String,Any>) {
        setProteinIntake.value = Resource.loading()

        webService.setProteinIntake(hashMap).enqueue(object : Callback<ApiResponse<WaterIntake>> {

            override fun onResponse(call: Call<ApiResponse<WaterIntake>>,
                                    response: Response<ApiResponse<WaterIntake>>) {
                if (response.isSuccessful) {
                    setProteinIntake.value = Resource.success(response.body()?.data)
                } else {
                    setProteinIntake.value = Resource.error(
                        ApiUtils.getError(response.code(),
                            response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<WaterIntake>>, throwable: Throwable) {
                setProteinIntake.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }

    fun getProteinIntake(hashMap: HashMap<String,String>) {
        getProteinIntake.value = Resource.loading()

        webService.getProteinIntake(hashMap).enqueue(object : Callback<ApiResponse<WaterIntake>> {

            override fun onResponse(call: Call<ApiResponse<WaterIntake>>,
                                    response: Response<ApiResponse<WaterIntake>>) {
                if (response.isSuccessful) {
                    getProteinIntake.value = Resource.success(response.body()?.data)
                } else {
                    getProteinIntake.value = Resource.error(
                        ApiUtils.getError(response.code(),
                            response.errorBody()?.string()))
                }
            }

            override fun onFailure(call: Call<ApiResponse<WaterIntake>>, throwable: Throwable) {
                getProteinIntake.value = Resource.error(ApiUtils.failure(throwable))
            }

        })
    }
}