package com.consultantvendor.ui.dashboard.home.prescription.digital

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.consultantvendor.R
import com.consultantvendor.ui.dashboard.home.prescription.model.InsuranceResponse
import com.consultantvendor.ui.dashboard.home.prescription.model.ResponseInsurance
import com.consultantvendor.ui.dashboard.home.reports.AddReportFragment

class InsuranceAdapter(private val fragment: AddReportFragment,private var items: List<ResponseInsurance>) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val view: View
        val vh: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(fragment.requireContext()).inflate(R.layout.item_spinner, parent, false)
            vh = ViewHolder(view)
            view?.tag = vh
        } else {
            view = convertView
            vh = view.tag as ViewHolder
        }

        vh.tvTitle.text = items[position].titleEN


       /* if (fragment.userRepository.getUserLanguage()=="en"){
            vh.tvTitle.text = items[position].titleEN
        }
        else{
            vh.tvTitle.text = items[position].titleAR
        }*/


        return view
    }

    override fun getItem(position: Int): Any? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getCount(): Int {
        return items.size
    }

    private class ViewHolder(view: View?) {
        val tvTitle: TextView = view?.findViewById(R.id.tvSpinner) as TextView
    }

}