package com.consultantvendor.ui.loginSignUp.login

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.UserSession
import com.consultantvendor.databinding.BottomLoginBinding
import com.consultantvendor.di.DaggerBottomSheetDialogFragment
import com.consultantvendor.utils.MultiLoginManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import androidx.core.graphics.drawable.toDrawable
import com.consultantvendor.ui.dashboard.HomeActivity
import com.consultantvendor.utils.longToast

class BottomLoginFragment() : DaggerBottomSheetDialogFragment() {

    private lateinit var binding: BottomLoginBinding
    private var adapter: LoginUserAdapter? = null
    private lateinit var users: List<UserSession>

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
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
        listeners()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun setupPreviousUsers() {
        users = MultiLoginManager.getUsers(requireContext())
        if (users.isNotEmpty()) {
//            adapter = LoginUserAdapter(users) { selectedIndex ->
//                users.forEachIndexed { index, option ->
//                    option.isSelect = index == selectedIndex
//                }
//                (activity as? HomeActivity)?.hitApiLogin(users[selectedIndex])
//                dialog?.dismiss()
//                adapter?.notifyDataSetChanged()
//
//            }

            adapter = LoginUserAdapter(users) { selectedIndex ->
                users.forEachIndexed { index, option ->
                    option.isSelect = index == selectedIndex
                }
                adapter?.notifyDataSetChanged()

                (activity as? HomeActivity)?.hitApiLogin(users[selectedIndex])
                dialog?.dismiss()
            }

        }

        binding.rvUser.adapter = adapter

    }

    private fun listeners() {
        binding.ivClose.setOnClickListener {
            dialog?.dismiss()
        }

        binding.clMain.setOnClickListener {
            if (users.size > 3){
                requireActivity().longToast(getString(R.string.maximum_user_limit))
            }else{
                startActivity(
                    Intent(activity, LoginActivity::class.java))
                dialog?.dismiss()
            }

        }
    }

//    companion object {
//        fun newInstance(): BottomLoginFragment {
//            return BottomLoginFragment()
//        }
//    }
}