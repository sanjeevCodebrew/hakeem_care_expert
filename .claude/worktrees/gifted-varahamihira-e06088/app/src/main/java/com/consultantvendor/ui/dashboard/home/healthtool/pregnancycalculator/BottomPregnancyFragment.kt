package com.consultantvendor.ui.dashboard.home.healthtool.pregnancycalculator

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.databinding.BottomPregnancyBinding
import com.consultantvendor.di.DaggerBottomSheetDialogFragment
import com.consultantvendor.utils.DateFormat
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.PrefsManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.util.*
import javax.inject.Inject


class BottomPregnancyFragment(private val dueDate: Long) : DaggerBottomSheetDialogFragment() {

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    @Inject
    lateinit var prefsManager: PrefsManager

    private lateinit var binding: BottomPregnancyBinding


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        dialog.setCanceledOnTouchOutside(true)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        binding = DataBindingUtil.inflate(inflater, R.layout.bottom_pregnancy, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setStyle(DialogFragment.STYLE_NORMAL, android.R.style.Theme_Black_NoTitleBar_Fullscreen)

        initialise()
        listeners()
        dataSetup()
    }

    private fun initialise() {

    }

    private fun listeners() {

    }

    private fun dataSetup() {
        val pregnancyDays = 280
        val date = DateUtils.dateFormatFromMillis(DateFormat.MON_DATE_YEAR, dueDate)
        binding.tvDueDate.text = getString(R.string.your_due_date_is_s, date)

        val pregnancyStartDate = calculateDate(-pregnancyDays)

        val calendarCurrent = Calendar.getInstance(Locale.ENGLISH)
        val diff: Long = calendarCurrent.timeInMillis - pregnancyStartDate
        val dayCount = diff.toFloat() / (24 * 60 * 60 * 1000)

        val weeks = (dayCount / 7).toInt()
        val days = (dayCount % 7).toInt()
        binding.tvPregnancyDate.text = getString(R.string.your_are_s_s_pregnant, weeks.toString(), days.toString())

        val trimesterDifference = pregnancyDays / 3
        val trimester1StartDate = pregnancyStartDate
        val trimester1EndDate = calculateDateSetTime(pregnancyStartDate, trimesterDifference - 3)
        val trimester2StartDate = calculateDateSetTime(trimester1EndDate, 1)
        val trimester2EndDate = calculateDateSetTime(trimester2StartDate, trimesterDifference + 4)
        val trimester3StartDate = calculateDateSetTime(trimester2EndDate, 1)
        val trimester3EndDate = calculateDateSetTime(trimester3StartDate, trimesterDifference - 2)

        binding.tvTimeline1Date.text = "${DateUtils.dateFormatFromMillis(DateFormat.MON_DATE, trimester1StartDate)} ${getString(R.string.to)} ${DateUtils.dateFormatFromMillis(DateFormat.MON_DATE, trimester1EndDate)}"
        binding.tvTimeline2Date.text = "${DateUtils.dateFormatFromMillis(DateFormat.MON_DATE, trimester2StartDate)} ${getString(R.string.to)} ${DateUtils.dateFormatFromMillis(DateFormat.MON_DATE, trimester2EndDate)}"
        binding.tvTimeline3Date.text = "${DateUtils.dateFormatFromMillis(DateFormat.MON_DATE, trimester3StartDate)} ${getString(R.string.to)} ${DateUtils.dateFormatFromMillis(DateFormat.MON_DATE, trimester3EndDate)}"

        when {
            calendarCurrent.timeInMillis < trimester1EndDate -> { //Lies in 1st Trimester
                val percentage = (calendarCurrent.timeInMillis - trimester1StartDate) * 100 / (trimester1EndDate - trimester1StartDate)
                binding.progress1st.progress = percentage.toInt()
                binding.progress2nd.progress = 0
                binding.progress3rd.progress = 0
            }
            calendarCurrent.timeInMillis < trimester2EndDate -> { //Lies in 2nd Trimester
                binding.progress1st.progress = 100
                val percentage = (calendarCurrent.timeInMillis - trimester2StartDate) * 100 / (trimester2EndDate - trimester2StartDate)
                binding.progress2nd.progress = percentage.toInt()
                binding.progress3rd.progress = 0
            }
            else -> { //Lies in 3rd Trimester
                binding.progress1st.progress = 100
                binding.progress2nd.progress = 100
                val percentage = (calendarCurrent.timeInMillis - trimester3StartDate) * 100 / (trimester3EndDate - trimester3StartDate)
                binding.progress3rd.progress = percentage.toInt()
            }
        }

        binding.tvYourBaby.text = getString(R.string.your_baby_will_be_a_s, getZodiacSign())
    }

    private fun calculateDate(days: Int): Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        val dateSelected = DateUtils.dateFormatFromMillis(DateFormat.MON_DATE_YEAR, dueDate)
        val date = DateUtils.dateFormatChange(DateFormat.MON_DATE_YEAR, DateFormat.DATE, dateSelected).toInt()
        val month = DateUtils.dateFormatChange(DateFormat.MON_DATE_YEAR, DateFormat.MONTH, dateSelected).toInt()
        val year = DateUtils.dateFormatChange(DateFormat.MON_DATE_YEAR, DateFormat.YEAR, dateSelected).toInt()
        calendar.set(year, (month - 1), date)

        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.timeInMillis
    }

    private fun calculateDateSetTime(timeInMilis: Long, days: Int): Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = timeInMilis
        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.timeInMillis
    }

    private fun getZodiacSign(): String {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        calendar.timeInMillis = dueDate
        val day = calendar.get(Calendar.DATE)
        val month = calendar.get(Calendar.MONTH) + 1

        return when {
            day in 22..30 && month == 1 || day in 1..19 && month == 2 -> getString(R.string.aquarius)
            day in 20..29 && month == 2 || day in 1..20 && month == 3 -> getString(R.string.pisces)
            day in 21..31 && month == 3 || day in 1..20 && month == 4 -> getString(R.string.aries)
            day in 21..30 && month == 4 || day in 1..21 && month == 5 -> getString(R.string.taurus)
            day in 22..31 && month == 5 || day in 1..21 && month == 6 -> getString(R.string.gemini)
            day in 22..30 && month == 6 || day in 1..22 && month == 7 -> getString(R.string.cancer_)
            day in 23..31 && month == 7 || day in 1..22 && month == 8 -> getString(R.string.leo)
            day in 23..31 && month == 8 || day in 1..23 && month == 9 -> getString(R.string.virgo)
            day in 24..30 && month == 9 || day in 1..23 && month == 10 -> getString(R.string.libra)
            day in 24..31 && month == 10 || day in 1..22 && month == 11 -> getString(R.string.scorpio)
            day in 23..30 && month == 11 || day in 1..21 && month == 12 -> getString(R.string.sagittarius)
            else -> getString(R.string.capricorn)
        }
    }
}
