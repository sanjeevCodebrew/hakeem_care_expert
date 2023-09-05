package com.consultantvendor.ui.loginSignUp.insurance

import android.app.Activity
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.OnBackPressedCallback
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.appClientDetails
import com.consultantvendor.data.models.responses.CountryCity
import com.consultantvendor.data.models.responses.UserData
import com.consultantvendor.data.models.responses.appdetails.AppVersion
import com.consultantvendor.data.models.responses.appdetails.Insurance
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentInsauranceBinding
import com.consultantvendor.ui.AppVersionViewModel
import com.consultantvendor.ui.loginSignUp.LoginViewModel
import com.consultantvendor.ui.loginSignUp.category.CategoryFragment
import com.consultantvendor.utils.*
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.google.gson.Gson
import dagger.android.support.DaggerFragment
import javax.inject.Inject


class InsuranceFragment : DaggerFragment() {

    @Inject
    lateinit var appSocket: AppSocket

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var binding: FragmentInsauranceBinding

    private var rootView: View? = null

    private lateinit var progressDialog: ProgressDialog

    private lateinit var viewModel: LoginViewModel

    private lateinit var viewModelVersion: AppVersionViewModel

    private val items = ArrayList<Insurance>()

    private var userData: UserData? = null

    private var spinnerCountryAdapter: CustomSpinnerAdapter? = null

    private var spinnerStateAdapter: CustomSpinnerAdapter? = null

    private var spinnerCityAdapter: CustomSpinnerAdapter? = null

    private val itemsCountry = ArrayList<CountryCity>()

    private val itemsState = ArrayList<CountryCity>()

    private val itemsCity = ArrayList<CountryCity>()

    private var openFirstCountry = true

    private var openFirstState = true

    private var openFirstCity = true

    private var needCountry = false

    private var countryId = ""

    private var stateId = ""

    private var cityId = ""


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding =
                    DataBindingUtil.inflate(inflater, R.layout.fragment_insaurance, container, false)
            rootView = binding.root

            initialise()
            setUpdateInsurance()
            listeners()
            bindObservers()
        }
        return rootView
    }


    private fun initialise() {
        disableBackPress()
        viewModel = ViewModelProvider(this, viewModelFactory)[LoginViewModel::class.java]
        viewModelVersion = ViewModelProvider(this, viewModelFactory)[AppVersionViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())

        userData = userRepository.getUser()
        /*If need country List*/
        if (appClientDetails.country_id == null) {
            needCountry = true
            binding.ilCountry.visible()
        } else countryId = appClientDetails.country_id ?: ""
    }

    private fun disableBackPress() {
        activity?.onBackPressedDispatcher?.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // do nothing for disable back button
            }
        })
    }

    private fun setAdapters() {
        spinnerCountryAdapter = CustomSpinnerAdapter(requireContext(), itemsCountry)
        binding.spnCountry.adapter = spinnerCountryAdapter

        spinnerStateAdapter = CustomSpinnerAdapter(requireContext(), itemsState)
        binding.spnState.adapter = spinnerStateAdapter

        spinnerCityAdapter = CustomSpinnerAdapter(requireContext(), itemsCity)
        binding.spnCity.adapter = spinnerCityAdapter

        if (isConnectedToInternet(requireContext(), true)) {
            var hashMap = HashMap<String, String>()
            /*If need country list*/
            if (needCountry) {
                hashMap["type"] = CountryListType.COUNTRY
                viewModelVersion.countryCity(hashMap)
            }

            if (!needCountry || !userData?.profile?.country_id.isNullOrEmpty()) {
                if (needCountry)
                    countryId = userData?.profile?.country_id ?: ""

                hashMap = HashMap()
                hashMap["type"] = CountryListType.STATE
                hashMap["country_id"] = countryId

                viewModelVersion.countryCity(hashMap)
            }

            if (!userData?.profile?.state_id.isNullOrEmpty()) {
                stateId = userData?.profile?.state_id ?: ""

                hashMap = HashMap()
                hashMap["type"] = CountryListType.CITY
                hashMap["state_id"] = stateId

                viewModelVersion.countryCity(hashMap)
            }
        }
    }

    /*Get and update address insurance*/
    private fun setUpdateInsurance() {
        if (appClientDetails.clientFeaturesKeys.isAddress == true) {
            setAdapters()
            binding.groupAddress.visible()

            binding.etAddress.setText(userData?.profile?.address ?: "")
            binding.etCountry.setText(userData?.profile?.country ?: "")
            binding.etState.setText(userData?.profile?.state ?: "")
            binding.etCity.setText(userData?.profile?.city ?: "")

            userData?.custom_fields?.forEach {
                if (it.field_name == CustomFields.ZIP_CODE) {
                    binding.etZipCode.setText(it.field_value ?: "")
                    return@forEach
                }
            }

        } else {
            binding.groupAddress.gone()
        }

        items.addAll(appClientDetails.insurances ?: emptyList())

        if (appClientDetails.insurance == true) {
            binding.groupInsurance.visible()

            /*Check Selected Insurance*/
            if (userData?.insurance_enable == "1") {
                binding.cbYes.isChecked = true

                items.forEachIndexed { index, item ->
                    userData?.insurances?.forEachIndexed { _, insurance ->
                        if (item.id == insurance.id) {
                            items[index].isSelected = true
                            return@forEachIndexed
                        }
                    }
                }
            } else if (userData?.insurance_enable != null) {
                binding.cbNo.isChecked = true
                binding.groupInsurance.gone()
            }
        } else {
            binding.tvHaveInsurance.gone()
            binding.cbYes.gone()
            binding.cbNo.gone()
            binding.groupInsurance.gone()
        }

        val adapter = InsuranceAdapter(this, items)
        binding.rvInsurance.adapter = adapter
    }


    private fun listeners() {
//        binding.toolbar.setNavigationOnClickListener {
//            requireActivity().supportFragmentManager.popBackStack()
//        }

        binding.btnSubmit.setOnClickListener {
            binding.btnSubmit.hideKeyboard()

            checkValidations()
        }

        binding.etCountry.setOnClickListener {
            binding.etCountry.hideKeyboard()
            binding.spnCountry.performClick()
        }

        binding.spnCountry.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (openFirstCountry)
                    openFirstCountry = false
                else {
                    binding.etState.setText("")
                    binding.etCity.setText("")
                    if (position == 0) {
                        binding.etCountry.setText("")
                    } else {
                        binding.etCountry.setText(itemsCountry[position].name)
                        countryId = itemsCountry[position].id ?: ""

                        if (isConnectedToInternet(requireContext(), true)) {
                            val hashMap = HashMap<String, String>()
                            hashMap["type"] = CountryListType.STATE
                            hashMap["country_id"] = countryId
                            viewModelVersion.countryCity(hashMap)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        })

        binding.etState.setOnClickListener {
            binding.etState.hideKeyboard()
            binding.spnState.performClick()
        }

        binding.spnState.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long) {
                if (openFirstState)
                    openFirstState = false
                else {
                    binding.etCity.setText("")
                    if (position == 0) {
                        binding.etState.setText("")
                    } else {
                        binding.etState.setText(itemsState.get(position).name)
                        stateId = itemsState[position].id ?: ""

                        if (isConnectedToInternet(requireContext(), true)) {
                            val hashMap = HashMap<String, String>()
                            hashMap["type"] = CountryListType.CITY
                            hashMap["state_id"] = stateId

                            viewModelVersion.countryCity(hashMap)
                        }
                    }
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        })

        binding.etCity.setOnClickListener {
            binding.etCity.hideKeyboard()
            binding.spnCity.performClick()
        }

        binding.spnCity.onItemSelectedListener = (object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
            ) {
                if (openFirstCity)
                    openFirstCity = false
                else {
                    if (position == 0)
                        binding.etCity.setText("")
                    else {
                        cityId = itemsCity[position].id ?: ""
                        binding.etCity.setText(itemsCity.get(position).name)
                    }
                }
            }


            override fun onNothingSelected(parent: AdapterView<*>?) {
            }
        })

        binding.cbYes.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                binding.cbNo.isChecked = false
                binding.groupInsurance.visible()
            } else {
                binding.groupInsurance.gone()
            }
        }

        binding.cbNo.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                binding.cbYes.isChecked = false
                binding.groupInsurance.gone()
            }
        }
    }

    private fun checkValidations() {
        /*Get if insurance selected*/
        var idsInsurance = ""
        items.forEach {
            if (it.isSelected)
                idsInsurance += it.id + ","
        }

        when {
            appClientDetails.clientFeaturesKeys.isAddress == true && binding.etAddress.text.toString().isEmpty() -> {
                binding.etAddress.showSnackBar(getString(R.string.address))
            }
            needCountry && appClientDetails.clientFeaturesKeys.isAddress == true && binding.etCountry.text.toString().isEmpty() -> {
                binding.etCountry.showSnackBar(getString(R.string.select_country))
            }
            appClientDetails.clientFeaturesKeys.isAddress == true && binding.etState.text.toString().isEmpty() -> {
                binding.etState.showSnackBar(getString(R.string.select_state))
            }
            appClientDetails.clientFeaturesKeys.isAddress == true && binding.etCity.text.toString().isEmpty() -> {
                binding.etCity.showSnackBar(getString(R.string.select_city))
            }
            appClientDetails.clientFeaturesKeys.isAddress == true && binding.etZipCode.text.toString().isEmpty() -> {
                binding.etZipCode.showSnackBar(getString(R.string.zip))
            }
            appClientDetails.insurance == true && (!binding.cbYes.isChecked && !binding.cbNo.isChecked) -> {
                binding.etCity.showSnackBar(getString(R.string.do_you_have_insurance))
            }
            appClientDetails.insurance == true && (binding.cbYes.isChecked && idsInsurance.isEmpty()) -> {
                binding.etCity.showSnackBar(getString(R.string.select_insurance))
            }
            appClientDetails.insurance == true && (!binding.cbTerm1.isChecked || !binding.cbTerm3.isChecked
                    || !binding.cbTerm3.isChecked) -> {
                binding.etCity.showSnackBar(getString(R.string.check_all_terms))
                binding.nsvInsurance.fullScroll(View.FOCUS_DOWN)
            }
            else -> {

                val hashMap = HashMap<String, Any>()
                hashMap["name"] = userRepository.getUser()?.name ?:""
                hashMap["address"] = binding.etAddress.text.toString()
                hashMap["country"] = countryId
                hashMap["state"] = stateId
                hashMap["city"] = cityId
                hashMap["insurance_enable"] = if (binding.cbYes.isChecked) "1" else "0"

                /*Get selected insurance*/
                if (binding.cbYes.isChecked) {
                    hashMap["insurances"] = idsInsurance.removeSuffix(",")
                }

                /*Check if zip id is there in custom fields*/

                appClientDetails.custom_fields?.service_provider?.forEach {
                    if (it.field_name == CustomFields.ZIP_CODE) {
                        val customer = ArrayList<Insurance>()
                        val item = it
                        item.field_value = binding.etZipCode.text.toString()

                        customer.add(item)

                        hashMap["custom_fields"] = Gson().toJson(customer)
                        return@forEach
                    }
                }

                viewModel.updateProfile(hashMap)
            }
        }
    }


    private fun bindObservers() {
        viewModel.updateProfile.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    prefsManager.save(USER_DATA, it.data)

                    requireActivity().setResult(Activity.RESULT_OK)

                    if (arguments?.containsKey(UPDATE_PROFILE) == true) {
                        requireActivity().finish()
                    } else
                        replaceFragment(requireActivity().supportFragmentManager,
                                CategoryFragment(), R.id.container)

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

        viewModelVersion.countryCity.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    when (it.data?.type) {
                        CountryListType.COUNTRY -> {
                            itemsCountry.clear()

                            val countryCity = CountryCity()
                            countryCity.name = getString(R.string.select_country)
                            itemsCountry.add(countryCity)

                            itemsCountry.addAll(it.data.country ?: emptyList())
                            spinnerCountryAdapter?.notifyDataSetChanged()

                        }
                        CountryListType.STATE -> {
                            itemsState.clear()

                            val countryCity = CountryCity()
                            countryCity.name = getString(R.string.select_state)
                            itemsState.add(countryCity)

                            itemsState.addAll(it.data.state ?: emptyList())
                            spinnerStateAdapter?.notifyDataSetChanged()

                        }
                        CountryListType.CITY -> {
                            itemsCity.clear()

                            val countryCity = CountryCity()
                            countryCity.name = getString(R.string.select_city)
                            itemsCity.add(countryCity)

                            itemsCity.addAll(it.data.city ?: emptyList())
                            spinnerCityAdapter?.notifyDataSetChanged()
                        }
                    }
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

}
