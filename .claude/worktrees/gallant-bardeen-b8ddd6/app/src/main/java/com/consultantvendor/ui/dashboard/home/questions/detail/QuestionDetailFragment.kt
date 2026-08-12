package com.consultantvendor.ui.dashboard.home.questions.detail

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Feed
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PushType
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.FragmentQuestionDetailBinding
import com.consultantvendor.ui.dashboard.home.questions.QuestionViewModel
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import kotlinx.android.synthetic.main.item_no_data.view.*
import javax.inject.Inject


class QuestionDetailFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentQuestionDetailBinding

    private var rootView: View? = null

    private var questionId: String? = null

    private var details: Feed? = null

    private var items = ArrayList<Feed>()

    private lateinit var adapter: AnswersAdapter

    private lateinit var viewModel: QuestionViewModel

    private lateinit var progressDialog: ProgressDialog

    private var isReceiverRegistered = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_question_detail,
                    container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[QuestionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.clLoader.setBackgroundResource(R.color.colorWhite)

        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_requests_empty_state)
        binding.clNoData.tvNoData.text = getString(R.string.no_answer)
        binding.clNoData.tvNoDataDesc.text = getString(R.string.no_answer_desc)

        questionId = arguments?.getString(EXTRA_REQUEST_ID)
        hitApi()
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, String>()
            hashMap["question_id"] = questionId ?: ""

            viewModel.getQuestionsDetails(hashMap)
        }
    }

    private fun setAdapter() {
        adapter = AnswersAdapter(this, items)
        binding.rvAnswer.adapter = adapter
    }


    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.ivSend.setOnClickListener {
            binding.etMessage.hideKeyboard()
            when {
                binding.etMessage.text.toString().trim().isEmpty() -> {
                    binding.etMessage.showSnackBar(getString(R.string.enter_message))
                }
                isConnectedToInternet(requireContext(), true) -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["question_id"] = questionId ?: ""
                    hashMap["answer"] = binding.etMessage.text.toString().trim()
                    viewModel.replyQuestion(hashMap)
                }
            }
        }
    }

    private fun setData() {
        binding.rlMessage.hideShowView(details?.you_answered == false)

        binding.tvName.text = getDoctorName(details?.created_by)
        loadImage(binding.ivPic, details?.created_by?.profile_image,
                R.drawable.image_placeholder)

        binding.tvTitle.text = details?.title
        binding.tvDec.text = details?.description

        items.clear()
        items.addAll(details?.answers ?: emptyList())
        adapter.notifyDataSetChanged()

        binding.clNoData.hideShowView(items.isEmpty())
    }

    private fun bindObservers() {
        viewModel.getQuestionsDetails.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.clLoader.setBackgroundResource(0)

                    details = it.data?.question
                    setData()

                }
                Status.ERROR -> {
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.visible()
                }
            }
        })

        viewModel.replyQuestion.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    hitApi()
                    binding.rlMessage.gone()
                }
                Status.ERROR -> {
                    binding.clLoader.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    binding.clLoader.visible()
                }
            }
        })
    }

    override fun onResume() {
        super.onResume()
        registerReceiver()
    }


    override fun onPause() {
        super.onPause()
        unregisterReceiver()
    }

    private fun registerReceiver() {
        if (!isReceiverRegistered) {
            val intentFilter = IntentFilter()
            intentFilter.addAction(PushType.FREE_EXPERT_ADVISE)
            LocalBroadcastManager.getInstance(requireContext())
                    .registerReceiver(refreshRequests, intentFilter)
            isReceiverRegistered = true
        }
    }

    private fun unregisterReceiver() {
        if (isReceiverRegistered) {
            LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(refreshRequests)
            isReceiverRegistered = false
        }
    }

    private val refreshRequests = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                PushType.FREE_EXPERT_ADVISE -> {
                    hitApi()
                }
            }
        }
    }
}
