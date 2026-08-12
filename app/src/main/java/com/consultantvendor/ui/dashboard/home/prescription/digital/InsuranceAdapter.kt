package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.consultantvendor.R
import com.consultantvendor.ui.dashboard.home.prescription.model.InsuranceResponse
import com.consultantvendor.ui.dashboard.home.prescription.model.ResponseInsurance

class InsuranceAdapter(
    private val fragment: androidx.fragment.app.Fragment,
    private var items: List<ResponseInsurance>
) : BaseAdapter() {

    var selectedname = ""

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: SpinnerViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.item_spinner, parent, false)
            vh = SpinnerViewHolder(view)
            view.tag = vh
        } else {
            view = convertView
            vh = view.tag as SpinnerViewHolder
        }
        val item = items[position]
        vh.tvTitle.text = if (item.code.isNotEmpty()) "${item.titleEN} (${item.code})" else item.titleEN
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: DropdownViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(fragment.requireContext())
                .inflate(R.layout.item_insurance_dropdown, parent, false)
            vh = DropdownViewHolder(view)
            view.tag = vh
        } else {
            view = convertView
            vh = view.tag as DropdownViewHolder
        }
        val item = items[position]
        vh.tvTitleEN.text = item.titleEN
        vh.tvTitleAR.text = item.titleAR
        vh.tvCode.text = fragment.requireContext().getString(R.string.code_label, item.code)
        vh.tvLicense.text = fragment.requireContext().getString(R.string.license_label, item.license)
        return view
    }

    override fun getItem(position: Int): Any? = null

    override fun getItemId(position: Int): Long = 0

    override fun getCount(): Int = items.size

    private class SpinnerViewHolder(view: View) {
        val tvTitle: TextView = view.findViewById(R.id.tvSpinner)
    }

    private class DropdownViewHolder(view: View) {
        val tvTitleEN: TextView = view.findViewById(R.id.tvTitleEN)
        val tvTitleAR: TextView = view.findViewById(R.id.tvTitleAR)
        val tvCode: TextView = view.findViewById(R.id.tvCode)
        val tvLicense: TextView = view.findViewById(R.id.tvLicense)
    }
}