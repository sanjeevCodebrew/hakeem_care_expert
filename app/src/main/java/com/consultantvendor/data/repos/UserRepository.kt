package com.consultantvendor.data.repos

import android.app.Activity
import androidx.lifecycle.MutableLiveData
import com.consultantvendor.ConsultantApplication
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.apis.WebService
import com.consultantvendor.data.models.PushData
import com.consultantvendor.data.models.responses.CommonDataModel
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.models.responses.appdetails.AppVersion
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.Resource
import com.consultantvendor.ui.loginSignUp.login.LoginActivity
import com.consultantvendor.utils.*
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber


@Singleton
class UserRepository @Inject constructor(
        private val app: ConsultantApplication,
        private val prefsManager: PrefsManager, private val webService: WebService
) {

    val groupCreatedCall = MutableLiveData<String>()
    val loginGuestUser = MutableLiveData<String>()
    val groupExitResponse = MutableLiveData<Pair<Boolean, String>>()
    val pushData = MutableLiveData<PushData>()
    val isNewNotification = MutableLiveData<Boolean>()


    fun isUserLoggedIn(): Boolean {
        val user = getUser()
        val appSetting = getAppSetting()

        return if (user?.id.isNullOrEmpty() || user?.name.isNullOrEmpty())
            false
//        else if (user?.categoryData == null)
//            false
//        else if (appSetting.insurance == true && user.insurance_enable == null)
//            false
        else if (appSetting.clientFeaturesKeys.isAddress == true && user?.profile?.address.isNullOrEmpty())
            false
        else
            true
    }


    fun getUser(): UserData? {
        return prefsManager.getObject(USER_DATA, UserData::class.java)
    }

    fun getAppSetting(): AppVersion {
        return prefsManager.getObject(APP_DETAILS, AppVersion::class.java) ?: AppVersion()
    }

    fun getUserLanguage(): String {
        return prefsManager.getString(USER_LANGUAGE, "")
    }

    fun getPushCallData(): PushData? {
        return prefsManager.getObject(PUSH_DATA, PushData::class.java)
    }

    fun pushTokenUpdate() {
        if (isUserLoggedIn()) {
            FirebaseMessaging.getInstance().token.addOnCompleteListener {
                if (it.isComplete) {

                    Timber.d(it.result)

                    val hashMap = HashMap<String, Any>()
                    hashMap["fcm_id"] = it.result

                    webService.updateFcmId(hashMap)
                            .enqueue(object : Callback<ApiResponse<UserData>> {

                                override fun onResponse(call: Call<ApiResponse<UserData>>, response: Response<ApiResponse<UserData>>) {
                                    if (response.isSuccessful) {
                                        Timber.e("Success")
                                    } else {
                                        Timber.e("Faliure")
                                    }
                                }

                                override fun onFailure(call: Call<ApiResponse<UserData>>, throwable: Throwable) {
                                    Timber.e("faliue 500")
                                }
                            })


                }
            }
        }
    }

    fun callStatus(requestId: String, call_id: String, callStatus: String) {
        val hashMap = HashMap<String, Any>()
        hashMap["request_id"] = requestId
        hashMap["call_id"] = call_id
        hashMap["status"] = callStatus

        webService.callStatus(hashMap)
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(call: Call<ApiResponse<CommonDataModel>>,
                            response: Response<ApiResponse<CommonDataModel>>) {
                        if (response.isSuccessful) {
                            Timber.e("Success")
                        } else {
                            Timber.e("Faliure")
                        }
                    }

                    override fun onFailure(call: Call<ApiResponse<CommonDataModel>>,
                            throwable: Throwable) {
                        Timber.e("faliue 500")
                    }
                })
    }

    fun getPages() {
        if (isUserLoggedIn()) {
            webService.getPages()
                .enqueue(object : Callback<ApiResponse<CommonDataModel>> {

                    override fun onResponse(
                        call: Call<ApiResponse<CommonDataModel>>,
                        response: Response<ApiResponse<CommonDataModel>>
                    ) {
                        if (response.isSuccessful) {
                            val commonData = Resource.success(response.body()?.data)

                            val appDetails = getAppSetting()
                            appDetails.pages = ArrayList()
                            appDetails.pages?.addAll(commonData.data?.pages ?: emptyList())

                            prefsManager.remove(APP_DETAILS)
                            prefsManager.save(APP_DETAILS, appDetails)
                            appClientDetails = getAppSetting()
                        } else {
                            Timber.e("Faliure")
                        }
                    }

                    override fun onFailure(
                        call: Call<ApiResponse<CommonDataModel>>,
                        throwable: Throwable
                    ) {
                        Timber.e("faliue 500")
                    }
                })

        }
    }

}

