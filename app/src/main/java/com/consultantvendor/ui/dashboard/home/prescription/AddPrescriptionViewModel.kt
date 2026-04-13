package com.consultantvendor.ui.dashboard.home.prescription

import androidx.lifecycle.ViewModel
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.DrugListResponse
import com.consultantvendor.data.models.MedicineResponse
import com.consultantvendor.data.models.requests.AddPrescription
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.DiagnosisResponse
import com.consultantvendor.data.models.responses.IcdDiagnosisListResponse
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.ApiUtils
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.di.SingleLiveEvent
import com.consultantvendor.ui.dashboard.home.prescription.model.InsuranceResponse
import com.consultantvendor.ui.dashboard.home.prescription.model.ResponseInsurance
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class AddPrescriptionViewModel @Inject constructor(private val webService: WebService) : ViewModel() {

    val prescreptions by lazy { SingleLiveEvent<Resource<UserData>>() }

    val addReports by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

    val getItemList by lazy { SingleLiveEvent<Resource<DrugListResponse>>() }

    val getdiagnosis by lazy { SingleLiveEvent<Resource<IcdDiagnosisListResponse>>() }

    val getInsurance by lazy { SingleLiveEvent<Resource<InsuranceResponse>>() }

    val addPrescription by lazy { SingleLiveEvent<Resource<CommonDataModel>>() }

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
            .enqueue(object : Callback<DrugListResponse> {

                override fun onResponse(
                    p0: Call<DrugListResponse?>,
                    response: Response<DrugListResponse?>
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
                    p0: Call<DrugListResponse?>,
                    p1: Throwable
                ) {
                    getItemList.value = Resource.error(ApiUtils.failure(p1))
                }

            })
    }


    fun getDiagnosis(hashMap: HashMap<String, String>) {
        getdiagnosis.value = Resource.loading()

        webService.getDiagnosisList(hashMap)
            .enqueue(object : Callback<IcdDiagnosisListResponse> {

                override fun onResponse(
                    p0: Call<IcdDiagnosisListResponse?>,
                    response: Response<IcdDiagnosisListResponse?>
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
                    p0: Call<IcdDiagnosisListResponse?>,
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

    fun addReports1(addPrescription: AddPrescription) {
        addReports.value = Resource.loading()

        webService.postMedicalReports1(addPrescription)
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



    fun getInsurance() {
        getInsurance.value = Resource.loading()

        webService.getInsurance()
            .enqueue(object : Callback<InsuranceResponse> {


                override fun onResponse(
                    p0: Call<InsuranceResponse?>,
                    response: Response<InsuranceResponse?>
                ) {
                    if (response.isSuccessful) {
                        getInsurance.value = Resource.success(response.body())
                    } else {
                        getInsurance.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(
                    p0: Call<InsuranceResponse?>,
                    p1: Throwable
                ) {
                    getInsurance.value = Resource.error(ApiUtils.failure(p1))
                }

            })
    }

    fun addPrescription(hashMap: HashMap<String, Any>) {
        addPrescription.value = Resource.loading()

        webService.postMedicalPrescription(hashMap)
            .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                                        response: Response<ApiResponse<CommonDataModel>>) {
                    if (response.isSuccessful) {
                        addPrescription.value = Resource.success(response.body()?.data)
                    } else {
                        addPrescription.value = Resource.error(
                            ApiUtils.getError(response.code(),
                                response.errorBody()?.string()))
                    }
                }

                override fun onFailure(call: Call<ApiResponse<CommonDataModel>>, throwable: Throwable) {
                    addPrescription.value = Resource.error(ApiUtils.failure(throwable))
                }

            })
    }

}