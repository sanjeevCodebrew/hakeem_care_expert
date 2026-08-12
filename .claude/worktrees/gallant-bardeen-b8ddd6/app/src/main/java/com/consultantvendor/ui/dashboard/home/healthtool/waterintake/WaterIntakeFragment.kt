package com.consultantvendor.ui.dashboard.home.healthtool.waterintake

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.requests.DatesAvailability
import com.consultantvendor.data.models.responses.WaterIntake
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentWaterIntakeBinding
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import dagger.android.support.DaggerFragment
import java.util.*
import javax.inject.Inject
import kotlin.collections.HashMap

class WaterIntakeFragment : DaggerFragment() {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: FragmentWaterIntakeBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var adapterIntake: WaterIntakeAdapter

    private var itemsIntake = ArrayList<DatesAvailability>()

    private var intakeSelected = 0

    private lateinit var viewModel: WaterIntakeViewModel

    var waterIntake: WaterIntake? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding =
                DataBindingUtil.inflate(inflater, R.layout.fragment_water_intake, container, false)
            rootView = binding.root

            initialise()
            listeners()
            setAdapter()
            bindObservers()
            hitApi()
        }
        return rootView
    }


    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[WaterIntakeViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.clLoader.setBackgroundResource(R.color.colorWhite)
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvDrink.setOnClickListener {
            when {
                waterIntake?.limit == null -> {
                    bottomDailyLimit()
                }
                intakeSelected == 0 -> {
                    binding.tvDrink.showSnackBar(getString(R.string.select_amount))
                }
                isConnectedToInternet(requireContext(), true) -> {
                    val hashMap = HashMap<String, Any>()
                    hashMap["quantity"] =
                        getWaterLiters(requireActivity(), intakeSelected, false)

                    viewModel.setWaterIntake(hashMap)
                }
            }
        }

        binding.tvSet.setOnClickListener {
            bottomDailyLimit()
        }
    }

    private fun bottomDailyLimit() {
        val fragment = BottomWaterLimitFragment(this)
        fragment.show(requireActivity().supportFragmentManager, fragment.tag)
    }

    private fun setAdapter() {
        itemsIntake.clear()
        var date: DatesAvailability
        var waterIntake = 0
        for (i in 0..4) {
            date = DatesAvailability()
            if (i == 0)
                date.intakeAmount = 100
            else {
                waterIntake += 250
                date.intakeAmount = waterIntake
            }
            itemsIntake.add(date)
        }

        adapterIntake = WaterIntakeAdapter(this, itemsIntake)
        binding.rvWaterIntake.adapter = adapterIntake

    }

    fun onWaterSelected(item: DatesAvailability) {
        binding.rvWaterIntake.smoothScrollToPosition(itemsIntake.indexOf(item))
        intakeSelected = item.intakeAmount ?: 0
    }

    fun setDailyLimit(limit: String) {
        if (isConnectedToInternet(requireContext(), true)) {
            val hashMap = HashMap<String, Any>()
            hashMap["limit"] = limit

            viewModel.setDailyLimit(hashMap)
        }
    }

    private fun hitApi() {
        if (isConnectedToInternet(requireContext(), true)) {
            viewModel.getWaterIntake(HashMap())
        }
    }

    private fun bindObservers() {
        viewModel.getWaterIntake.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.gone()
                    binding.clLoader.setBackgroundResource(0)

                    waterIntake = it.data
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

        viewModel.setWaterIntake.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    waterIntake = it.data
                    setData()

                    itemsIntake.forEachIndexed { index, datesAvailability ->
                        itemsIntake[index].isSelected = false
                    }
                    adapterIntake.notifyDataSetChanged()

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })

        viewModel.setDailyLimit.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    waterIntake = it.data
                    setData()

                }
                Status.ERROR -> {
                    progressDialog.setLoading(false)
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }
                Status.LOADING -> {
                    progressDialog.setLoading(true)
                }
            }
        })
    }

    private fun setData() {
        binding.tvSet.visible()
        binding.tvDailyLimitV.text =
            getWaterUnit(requireActivity(), waterIntake?.limit ?: 0.0, true)
        binding.tvIntakeV.text =
            getWaterUnit(requireActivity(), waterIntake?.today_intake ?: 0.0, true)

        binding.tvDaysCompletedV.text = waterIntake?.total_achieved_goal ?: getString(R.string.na)

        val todayIntake = waterIntake?.today_intake ?: 0.0
        val dailyLimit = waterIntake?.limit ?: 0.0

        var percentage = todayIntake / dailyLimit
        if (percentage > 1)
            percentage = 1.0
        binding.animationView.setMinAndMaxProgress(0.0f, percentage.toFloat())
        binding.animationView.playAnimation()

    }
}