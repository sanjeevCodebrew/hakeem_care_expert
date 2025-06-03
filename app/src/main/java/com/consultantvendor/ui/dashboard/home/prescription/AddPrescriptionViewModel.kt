package com.consultantvendor.ui.dashboard.home.prescription

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.MedicineResponse
import com.consultantvendor.data.models.requests.AddPrescription
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.DiagnosisResponse
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class AddPrescriptionViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val prescreptions by lazy { SingleLiveEvent<Resource<UserData>>() }

    val addReports by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val getItemList by lazy { SingleLiveEvent<Resource<MedicineResponse>>() }

    val getdiagnosis by lazy { SingleLiveEvent<Resource<DiagnosisResponse>>() }

    fun prescreptions(addPrescription: AddPrescription) {
        prescreptions.value = Resource.loading()

        webService.prescreptions(addPrescription)
                .enqueue(object : Callback<ApiResponse<UserData>> {

                    override fun onResponse(call: Call<ApiResponse<UserData>>,
                                            response: Response<ApiResponse<UserData>>) {
                        if (response.isSuccessful) {
                            prescreptions.value = Resource.success(response.body()?.data)
                        } else {
                            prescreptions.value = Resource.error(
                                    ApiUtils.getError(response.code(),
                                            response.errorBody()?.string()))
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<UserData>>, throwable: Throwable) {
                        prescreptions.value = Resource.error(ApiUtils.failure(throwable))
                    }

                })
    }

    fun getItemList(hashMap: HashMap<String, String>) {
        getItemList.value = Resource.loading()

        webService.getItemList(hashMap)
            .enqueue(object : Callback<MedicineResponse> {


                override fun onResponse(
                    p0: Call<MedicineResponse?>,
                    response: Response<MedicineResponse?>
                ) {
                    if (response.isSuccessful) {
                        getItemList.value = Resource.success(response.body())
                    } else {
                        getItemList.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(
                    p0: Call<MedicineResponse?>,
                    p1: Throwable
                ) {
                    getItemList.value = Resource.error(ApiUtils.failure(p1))
                }

            })
    }


    fun getDiagnosis(hashMap: HashMap<String, String>) {
        getdiagnosis.value = Resource.loading()

        webService.getDiagnosisList(hashMap)
            .enqueue(object : Callback<DiagnosisResponse> {


                override fun onResponse(
                    p0: Call<DiagnosisResponse?>,
                    response: Response<DiagnosisResponse?>
                ) {
                    if (response.isSuccessful) {
                        getdiagnosis.value = Resource.success(response.body())
                    } else {
                        getdiagnosis.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(
                    p0: Call<DiagnosisResponse?>,
                    p1: Throwable
                ) {
                    getdiagnosis.value = Resource.error(ApiUtils.failure(p1))
                }

            })
    }

    fun addReports(hashMap: HashMap<String, Any>) {
        addReports.value = Resource.loading()

        webService.postMedicalReports(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        addReports.value = Resource.success(response.body()?.data)
                    } else {
                        addReports.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    addReports.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }
}