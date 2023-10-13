package com.consultantvendor.ui.webview

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.webkit.*
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityWebViewBinding
import com.consultantvendor.ui.dashboard.wallet.WalletViewModel
import com.consultantvendor.utils.*
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject


class WebViewActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityWebViewBinding

    private lateinit var viewModel: WalletViewModel

    private var isReceiverRegistered = false

    private var transactionId = ""

    private var loadUrl = ""

    private val mHandler = Handler()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_web_view)

        initialise()
        bindViews()
        setListeners()
        bindObservers()
    }

    private fun initialise() {
        LocaleHelper.setLocale(this, userRepository.getUserLanguage(), prefsManager)
        viewModel = ViewModelProvider(this, viewModelFactory)[WalletViewModel::class.java]

        binding.tvHeader.text = intent.getStringExtra(LINK_TITLE)
        when {
            intent.hasExtra(PAYMENT_URL) -> {
                transactionId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""
                loadUrl = intent.getStringExtra(PAYMENT_URL) ?: ""
            }
            intent.hasExtra(PDF_LINK) -> {
                loadUrl = intent.getStringExtra(PDF_LINK) ?: ""
            }
            else -> {
                loadUrl = "${appClientDetails.domain_url}/${intent.getStringExtra(LINK_URL)}"
            }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun bindViews() {

        if (android.os.Build.VERSION.SDK_INT >= 21) {
            CookieManager.getInstance().setAcceptThirdPartyCookies(binding.webView, true)
        } else {
            CookieManager.getInstance().setAcceptCookie(true)
        }

        binding.webView.setBackgroundColor(Color.TRANSPARENT)
        binding.webView.settings.setSupportZoom(true)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)

                /*If Payment success*/
                if (url?.contains(PaymentFrom.AL_RAJHI_BANK_SUCCESS) == true) {
                    setResult(RESULT_OK)
                    finish()
//                    keepCheckingRequestStatus()
                }
            }
        }


//        binding.webView.settings.setAppCacheEnabled(true)
        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.settings.useWideViewPort = true
        binding.webView.setInitialScale(100)
        binding.webView.webChromeClient = WebChromeClient()


        // Return the app name after finish loading

        /*Ser headers*/
        val map = HashMap<String, String>()
        map["language"] = prefsManager.getString(USER_LANGUAGE, "en")
        binding.webView.loadUrl(loadUrl, map)

        binding.webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, progress: Int) {

                // Return the app name after finish loading
                if (progress == 100) {
                    binding.clLoader.gone()
                }
            }
        }
    }

    private fun setListeners() {
        binding.toolbar.setNavigationOnClickListener { finish() }

    }

    companion object {
        const val LINK_TITLE = "LINK_TITLE"
        const val LINK_URL = "LINK_URL"
        const val PAYMENT_URL = "PAYMENT_URL"
        const val PDF_LINK = "PDF_LINK"
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.BALANCE_ADDED)
            intentFilter.addAction(PushType.BALANCE_FAILED)
            LocalBroadcastManager.getInstance(this).registerReceiver(
                    callCancelledReceiver, intentFilter
            )
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(callCancelledReceiver)
            isReceiverRegistered = false
        }
    }

    private val callCancelledReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.getStringExtra(EXTRA_REQUEST_ID) == transactionId) {
                if (intent.action == PushType.BALANCE_ADDED) {
                    longToast(getString(R.string.transaction_success))
                    setResult(Activity.RESULT_OK)
                    finish()
                } else if (intent.action == PushType.BALANCE_FAILED) {
                    longToast(getString(R.string.transaction_failed))
                    finish()
                }
            }
        }
    }

    private fun bindObservers() {
        viewModel.requestCheck.observe(this, Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    if (it.data?.transactionCompleted == true) {
                        mHandler.removeCallbacksAndMessages(null)
                        setResult(RESULT_OK)
                        finish()
                    } else
                        keepCheckingRequestStatus()
                }
                Status.ERROR -> {
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                }
            }
        })
    }

    private fun keepCheckingRequestStatus() {
        mHandler.removeCallbacksAndMessages(null)
        mHandler.postDelayed({
            if (isConnectedToInternet(this, true)) {
                val hashMap = HashMap<String, String>()
                hashMap["transaction_id"] = transactionId
                hashMap["transaction_type"] = "wallet"
                viewModel.requestCheck(hashMap)
            }
        }, 5000)
    }
}
