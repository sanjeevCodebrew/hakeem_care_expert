package com.consultantvendor.ui.dashboard.settings.contactlist

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.ContactEmergency
import com.consultantvendor.data.network.ApiKeys.AFTER
import com.consultantvendor.data.network.ApiKeys.PER_PAGE
import com.consultantvendor.data.network.ApisRespHandler
import com.consultantvendor.data.network.PER_PAGE_LOAD
import com.consultantvendor.data.network.responseUtil.Status
import com.consultantvendor.databinding.ActivityListingToolbarBinding
import com.consultantvendor.utils.AppRequestCode
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.dialogs.ProgressDialog
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.hideShowView
import com.consultantvendor.utils.isConnectedToInternet
import com.consultantvendor.utils.visible
import com.google.gson.Gson
import com.wafflecopter.multicontactpicker.ContactResult
import com.wafflecopter.multicontactpicker.LimitColumn
import com.wafflecopter.multicontactpicker.MultiContactPicker
import dagger.android.support.DaggerFragment
import javax.inject.Inject

class ContactListFragment : DaggerFragment() {

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private lateinit var binding: ActivityListingToolbarBinding

    private var rootView: View? = null

    private lateinit var viewModel: ContactViewModel

    private lateinit var progressDialog: ProgressDialog

    private var items = ArrayList<ContactEmergency>()

    private lateinit var adapter: ContactListAdapter

    private var isLastPage = false

    private var isFirstPage = true

    private var isLoadingMoreItems = false


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.activity_listing_toolbar, container, false)
            rootView = binding.root

            initialise()
            setAdapter()
            listeners()
            bindObservers()
            hitApi(true)
        }
        return rootView
    }

    private fun initialise() {
        viewModel = ViewModelProvider(this, viewModelFactory)[ContactViewModel::class.java]
        progressDialog = ProgressDialog(requireActivity())
        binding.tvHeader.text = getString(R.string.contacts)
        binding.tvAdd.visible()


        binding.clNoData.ivNoData.setImageResource(R.drawable.ic_requests_empty_state)
        binding.clNoData.tvNoData.text = getString(R.string.no_contact)
        binding.clNoData.tvNoDataDesc.text = getString(R.string.no_contact_desc)
    }

    private fun setAdapter() {
        adapter = ContactListAdapter(this, items)
        binding.rvListing.adapter = adapter
    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            requireActivity().finish()
        }

        binding.tvAdd.setOnClickListener {
            if (checkContactPermission())
                pickContact()
            else
                requestContactPermission()
        }

        binding.swipeRefresh.setOnRefreshListener {
            hitApi(true)
        }

        binding.rvListing.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = binding.rvListing.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount - 1
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()

                if (!isLoadingMoreItems && !isLastPage && lastVisibleItemPosition >= totalItemCount) {
                    isLoadingMoreItems = true
                    hitApi(false)
                }
            }
        })
    }

    private fun hitApi(firstHit: Boolean) {
        if (isConnectedToInternet(requireContext(), true)) {
            if (firstHit) {
                isFirstPage = true
                isLastPage = false
            }

            val hashMap = HashMap<String, String>()

            if (!isFirstPage && items.isNotEmpty())
                hashMap[AFTER] = items[items.size - 1].id ?: ""

            hashMap[PER_PAGE] = PER_PAGE_LOAD.toString()

            viewModel.contactList(hashMap)
        } else
            binding.swipeRefresh.isRefreshing = false
    }

    private fun bindObservers() {
        viewModel.contactList.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    binding.clLoader.root.gone()
                    binding.swipeRefresh.isRefreshing = false

                    isLoadingMoreItems = false

                    val tempList = it.data?.contacts ?: emptyList()
                    if (isFirstPage) {
                        isFirstPage = false
                        items.clear()
                    }

                    items.addAll(tempList)
                    adapter.notifyDataSetChanged()

                    isLastPage = tempList.size < PER_PAGE_LOAD
                    adapter.setAllItemsLoaded(isLastPage)

                    binding.clNoData.root.hideShowView(items.isEmpty())
                }

                Status.ERROR -> {
                    isLoadingMoreItems = false
                    adapter.setAllItemsLoaded(true)

                    binding.swipeRefresh.isRefreshing = false
                    binding.clLoader.root.gone()
                    ApisRespHandler.handleError(it.error, requireActivity(), prefsManager)
                }

                Status.LOADING -> {
                    if (!isLoadingMoreItems && !binding.swipeRefresh.isRefreshing)
                        binding.clLoader.root.visible()
                }
            }
        })

        viewModel.addContact.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    hitApi(true)
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

        viewModel.deletContact.observe(requireActivity(), Observer {
            it ?: return@Observer
            when (it.status) {
                Status.SUCCESS -> {
                    progressDialog.setLoading(false)

                    hitApi(true)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                AppRequestCode.SELECT_CONTACT -> {
                    val results: List<ContactResult> = MultiContactPicker.obtainResult(data)
                    Log.d("MyTag", Gson().toJson(results).toString())

                    hitApiContacts(results)
                }
            }
        }
    }

    private fun hitApiContacts(results: List<ContactResult>) {
        if (isConnectedToInternet(requireContext(), true)) {
            val contactList = ContactEmergency()
            contactList.contacts = ArrayList()
            results.forEach {
                val contact = ContactEmergency()
                contact.name = it.displayName
                contact.phone_numbers = ArrayList()
                it.phoneNumbers.forEach {
                    contact.phone_numbers?.add(ContactEmergency(phone = it.number, type_label = it.typeLabel))
                }

                contactList.contacts?.add(contact)

            }
            viewModel.addContact(contactList)
        }
    }

    fun deleteContact(contact: ContactEmergency) {
        if (isConnectedToInternet(requireContext(), true)) {
            AlertDialog.Builder(requireContext())
                .setCancelable(false)
                .setTitle(getString(R.string.delete))
                .setMessage(getString(R.string.delete_contact, contact.name))
                .setPositiveButton(getString(R.string.delete)) { dialog, which ->
                    val hashMap = HashMap<String, Any>()
                    hashMap["id"] = contact.id ?: ""
                    viewModel.deletContact(hashMap)
                }.setNegativeButton(getString(R.string.no)) { dialog, which ->
                }.show()
        }
    }

    private fun checkContactPermission(): Boolean {
        //check if permission was granted/allowed or not, returns true if granted/allowed, false if not
        return ContextCompat.checkSelfPermission(
            requireContext(),
            android.Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestContactPermission() {
        //request the READ_CONTACTS permission
        val permission = arrayOf(android.Manifest.permission.READ_CONTACTS)
        ActivityCompat.requestPermissions(requireActivity(), permission, 100)
    }

    private fun pickContact() {
        //intent ti pick contact
        MultiContactPicker.Builder(this) //Activity/fragment context
            .theme(R.style.AppTheme) //Optional - default: MultiContactPicker.Azure
            .hideScrollbar(false) //Optional - default: false
            .showTrack(true) //Optional - default: true
            .searchIconColor(Color.WHITE) //Option - default: White
            .setChoiceMode(MultiContactPicker.CHOICE_MODE_MULTIPLE) //Optional - default: CHOICE_MODE_MULTIPLE
            .handleColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary)) //Optional - default: Azure Blue
            .bubbleColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary)) //Optional - default: Azure Blue
            .bubbleTextColor(Color.WHITE) //Optional - default: White
            .setTitleText(getString(R.string.select_contacts)) //Optional - default: Select Contacts
            .setSelectedContacts("10", "5") //Optional - will pre-select contacts of your choice. String... or List<ContactResult>
            .setLoadingType(MultiContactPicker.LOAD_ASYNC) //Optional - default LOAD_ASYNC (wait till all loaded vs stream results)
            .limitToColumn(LimitColumn.NONE) //Optional - default NONE (Include phone + email, limiting to one can improve loading time)
            .setActivityAnimations(
                android.R.anim.fade_in, android.R.anim.fade_out,
                android.R.anim.fade_in,
                android.R.anim.fade_out
            ) //Optional - default: No animation overrides
            .showPickerForResult(AppRequestCode.SELECT_CONTACT)
    }
}
