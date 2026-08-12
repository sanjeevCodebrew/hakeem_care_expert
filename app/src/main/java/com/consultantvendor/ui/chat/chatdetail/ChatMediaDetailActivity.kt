package com.consultantvendor.ui.chat.chatdetail

import android.os.Bundle
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.ActivityChatMediaBinding
import com.consultantvendor.ui.chat.ChatViewModel
import com.consultantvendor.utils.EXTRA_REQUEST_ID
import com.consultantvendor.utils.DocType
import com.consultantvendor.utils.ImageFolder
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.RECIEVER_ID
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.getImageBaseUrl
import com.denzcoskun.imageslider.constants.ScaleTypes
import com.denzcoskun.imageslider.models.SlideModel
import dagger.android.support.DaggerAppCompatActivity
import javax.inject.Inject

class ChatMediaDetailActivity : DaggerAppCompatActivity() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityChatMediaBinding
    private lateinit var progressDialog: ProgressDialog
    private lateinit var viewModel: ChatViewModel

    private val imageList = ArrayList<SlideModel>()
    private var requestId = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_chat_media)

        viewModel = ViewModelProvider(this, viewModelFactory)[ChatViewModel::class.java]
        progressDialog = ProgressDialog(this)

        requestId = intent.getStringExtra(EXTRA_REQUEST_ID) ?: ""

        fetchMedia()
        observeMedia()
    }

    private fun fetchMedia() {
        val params = HashMap<String, String>()
        params["request_id"] = requestId
        params["media_type"] = DocType.IMAGE
        viewModel.getChatMedia(params)
    }

    private fun observeMedia() {
        viewModel.chatMedia.observe(this) {
            it ?: return@observe
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)
                    it.data?.media?.forEach { media ->
                        val url = getImageBaseUrl(ImageFolder.UPLOADS, media.image_url)
                        imageList.add(SlideModel(url, ""))
                    }
                    binding.imageSlider.setImageList(imageList, ScaleTypes.CENTER_CROP)
                    binding.imageSlider.stopSliding()
                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, this, prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        }
    }
}
