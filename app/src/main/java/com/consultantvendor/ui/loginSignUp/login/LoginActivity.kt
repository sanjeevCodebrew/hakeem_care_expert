package com.consultantvendor.ui.loginSignUp.login

import android.content.Intent
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.ApiResponse
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityLoginBinding
import com.consultantvendor.databinding.FragmentLoginBinding
import com.consultantvendor.ui.dashboard.HomeActivity
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.ui.loginSignUp.category.CategoryFragment
import com.consultantvendor.ui.loginSignUp.insurance.InsuranceFragment
import com.consultantvendor.ui.loginSignUp.loginemail.LoginEmailFragment
import com.consultantvendor.ui.loginSignUp.verifyotp.VerifyOTPFragment
import com.consultantvendor.ui.loginSignUp.welcome.WelcomeFragment
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.google.firebase.messaging.FirebaseMessaging
import dagger.android.support.DaggerAppCompatActivity
import dagger.android.support.DaggerFragment
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import javax.inject.Inject

class LoginActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: ActivityLoginBinding

    private lateinit var progressDialog: ProgressDialog

    private var userRepository: UserRepository?=null

    private var fcmId  = ""

    private lateinit var viewModel: LoginViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_login)

        initialise()
        listeners()
        bindObservers()
    }

    private fun initialise() {
//        binding.ccpCountryCode.setCountryForNameCode(appClientDetails.country_name_code ?: "IN")

        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        progressDialog = ProgressDialog(this)
        binding.tvTerms.movementMethod = LinkMovementMethod.getInstance()
        binding.tvTerms.setText(setAcceptTerms(this), TextView.BufferType.SPANNABLE)

     /*   if (arguments?.containsKey(WelcomeFragment.EXTRA_SIGNUP) == true) {
            binding.tvLoginScreen.gone()
            binding.tvLoginTitle.gone()
            binding.tvTerms.visible()
        } else if (arguments?.containsKey(UPDATE_NUMBER) == true) {
            binding.tvTitle.text = getString(R.string.update)
            binding.tvLoginScreen.gone()
            binding.tvLoginTitle.gone()
            binding.tvTerms.gone()
            binding.tvTerms.gone()
        }*/
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (supportFragmentManager.backStackEntryCount > 0)
                supportFragmentManager.popBackStack()
            else
                finish()
        }

        binding.tvLoginScreen.setOnClickListener {
            supportFragmentManager.popBackStack()
            replaceFragment(supportFragmentManager, LoginEmailFragment(), R.id.container)
        }

        FirebaseMessaging.getInstance().token.addOnCompleteListener {
            if (it.isComplete) {
                Log.d("FCMToken", it.result)
                fcmId = it.result
            }
        }

        binding.ivNext.setOnClickListener {
            when {
                binding.etMobileNumber.text.toString().isEmpty() || binding.etMobileNumber.text.toString().length < 6 -> {
                    binding.etMobileNumber.showSnackBar(getString(R.string.enter_moh_number))
                }
                binding.tvTerms.visibility==View.VISIBLE && !binding.tvTerms.isChecked -> {
                    binding.tvTerms.showSnackBar(getString(R.string.agree_to_terms))
                }
                isConnectedToInternet(this, true) -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["moh_number"] = binding.etMobileNumber.text.toString()
                    hashMap["fcm_id"] = fcmId
                    viewModel.drLogin(hashMap)
                }
            }
        }
    }

    private fun bindObservers() {

          viewModel.drLogin.observe(this, Observer {
          it ?: return@Observer
          when (it.status) {
              Status.SUCCESS -> {
                  progressDialog.setLoading(false)
                  prefsManager.save(USER_DATA, it.data)
                  prefsManager.save(IS_USER_LOGIN,it.data?.is_login_access)
                  startActivity(Intent(this, HomeActivity::class.java))

              }
              Status.ERROR -> {
                  progressDialog.setLoading(false)
                  ApisRespHandler.handleError(it.error, this, prefsManager)
              }
              Status.LOADING -> {
                  progressDialog.setLoading(true)
              }
          }
      })
    }
}