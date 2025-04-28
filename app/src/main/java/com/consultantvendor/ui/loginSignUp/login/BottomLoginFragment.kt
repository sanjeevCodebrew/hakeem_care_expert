package com.consultantvendor.ui.loginSignUp.login

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.UserSession
import com.consultantvendor.databinding.BottomLoginBinding
import com.consultantvendor.di.DaggerBottomSheetDialogFragment
import com.consultantvendor.utils.MultiLoginManager
import com.consultantvendor.utils.SessionManager
import com.consultantvendor.utils.visible
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog

class BottomLoginFragment(private val activity: Activity) : DaggerBottomSheetDialogFragment() {

    private lateinit var binding: BottomLoginBinding

    private  var adapter: LoginUserAdapter?=null
    private var sessions = mutableListOf<UserSession>()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        return dialog
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_login, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        setupPreviousUsers()
        listners()

    }

    private fun setupPreviousUsers() {
        val users = MultiLoginManager.getUsers(requireContext())
        if (users.isNotEmpty()) {
            adapter = LoginUserAdapter(users) { selectedIndex ->
                val item= users[selectedIndex]
//                activity.binding.etMobileNumber.setText(item.moh)
//                activity.binding.ivNext.performClick()
                 reloadSessions()
//                Toast.makeText(requireContext(), "Switched to ${item.username}", Toast.LENGTH_SHORT).show()
            }
        }
        binding.rvUser.adapter = adapter
    }

    private fun listners() {
        binding.ivClose.setOnClickListener {
            dialog?.dismiss()
        }
    }

    private fun reloadSessions() {
        sessions.clear()
        sessions.addAll(SessionManager.getSessions(requireActivity()))
        adapter?.notifyDataSetChanged()
    }
}