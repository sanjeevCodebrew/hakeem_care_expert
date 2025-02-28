package com.consultantvendor.ui.dashboard.home.healthtool.pregnancycalculator

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import com.consultantvendor.R
import com.consultantvendor.data.models.responses.Filter
import com.consultantvendor.data.repos.UserRepository
import com.consultantvendor.databinding.FragmentPregnancyCalculatorBinding
import com.consultantvendor.ui.adapter.CheckItemAdapter
import com.consultantvendor.utils.DateFormat
import com.consultantvendor.utils.DateUtils
import com.consultantvendor.utils.OnDateSelected
import com.consultantvendor.utils.PrefsManager
import com.consultantvendor.utils.gone
import com.consultantvendor.utils.showSnackBar
import com.consultantvendor.utils.visible
import dagger.android.support.DaggerFragment
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject


class PregnancyCalculatorFragment : DaggerFragment(), OnDateSelected {

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var prefsManager: PrefsManager

    @Inject
    lateinit var viewModelFactory: ViewModelProvider.Factory

    private var rootView: View? = null

    private lateinit var binding: FragmentPregnancyCalculatorBinding

    private lateinit var adapterCalMethod: CheckItemAdapter

    private var itemsCalMethod = ArrayList<Filter>()

    private lateinit var adapterIvf: CheckItemAdapter

    private var itemsIvf = ArrayList<Filter>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (rootView == null) {
            binding = DataBindingUtil.inflate(inflater, R.layout.fragment_pregnancy_calculator, container, false)
            rootView = binding.root

            initialisation()
            listeners()
            setAdapter()
        }
        return rootView
    }

    private fun initialisation() {

    }

    private fun listeners() {
        binding.toolbar.setNavigationOnClickListener {
            if (requireActivity().supportFragmentManager.backStackEntryCount > 0)
                requireActivity().supportFragmentManager.popBackStack()
            else
                requireActivity().finish()
        }

        binding.tvDate.setOnClickListener {
            var calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, -3)
            val min = calendar.timeInMillis
            calendar = Calendar.getInstance()
            calendar.set(Calendar.MONTH, +3)
            val max = calendar.timeInMillis
            DateUtils.openDatePicker(requireActivity(), this, null, null)
        }

        binding.tvCycle.setOnClickListener {
            binding.spnCycleLength.performClick()
        }

        binding.spnCycleLength.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?, position: Int, id: Long
            ) {
                binding.tvCycle.text = binding.spnCycleLength.selectedItem.toString()
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {

            }
        }

        binding.tvWeek.setOnClickListener {
            binding.spnWeek.performClick()
        }

        binding.spnWeek.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?, position: Int, id: Long
            ) {
                binding.tvWeek.text = binding.spnWeek.selectedItem.toString()
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {

            }
        }

        binding.tvDay.setOnClickListener {
            binding.spnDays.performClick()
        }

        binding.spnDays.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>,
                selectedItemView: View?, position: Int, id: Long
            ) {
                binding.tvDay.text = binding.spnDays.selectedItem.toString()
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {

            }
        }

        binding.tvCalculate.setOnClickListener {
            if (binding.tvDate.text.toString().isEmpty()) {
                itemsCalMethod.forEachIndexed { index, filter ->
                    when (filter.isSelected) {
                        (index == 0) -> binding.tvDate.showSnackBar(getString(R.string.the_first_day_of_your_last_period))
                        (index == 1) -> binding.tvDate.showSnackBar(getString(R.string.date_of_conception))
                        (index == 2) -> binding.tvDate.showSnackBar(getString(R.string.date_of_transfer))
                        (index == 3) -> binding.tvDate.showSnackBar(getString(R.string.date_of_ultrasound))
                        else -> {

                        }
                    }
                }
                return@setOnClickListener
            } else {
                var dateTime = 0L
                itemsCalMethod.forEachIndexed { index, filter ->
                    if (filter.isSelected) {
                        when (index) {
                            0 -> dateTime = calculateViaLastPeriod()
                            1 -> dateTime = calculateViaConceptionDate()
                            2 -> dateTime = calculateViaIVF()
                            3 -> dateTime = calculateViaUltrasound()
                        }
                        return@forEachIndexed
                    }
                }

                val fragment = BottomPregnancyFragment(dateTime)
                fragment.show(requireActivity().supportFragmentManager, fragment.tag)

            }
        }
    }

    private fun calculateViaLastPeriod(): Long {
        val pregnancyDays = 280

        when (binding.spnCycleLength.selectedItemPosition) {
            0 -> {
                //Period cycle not known
                return calculateDate(pregnancyDays)
            }

            1 -> {
                //Default menstural cycle 21 days then subtract 7 days
                return calculateDate(pregnancyDays - 7)
            }

            else -> {
                //Period Cycle Range 22 to 35 days
                //if mestural cycle is more than 21 days than subtract your menstural cycle days count by 21 (default menstural cycle)
                //And add it to date generated by adding 280 days

                val daysToAdd = (binding.spnCycleLength.selectedItemPosition + 20) - 21
                return calculateDate(pregnancyDays + daysToAdd - 7)
            }
        }
    }

    private fun calculateViaConceptionDate(): Long {
        //Add 266 days to conception date
        return calculateDate(266)
    }

    private fun calculateViaIVF(): Long {
        if (itemsIvf[0].isSelected)
            return calculateDate((266 - 3))
        else if (itemsIvf[1].isSelected)
            return calculateDate((266 - 5))
        else
            return calculateDate((266 - 3))
    }

    private fun calculateViaUltrasound(): Long {
        val pregnancyDays = 280
        val embryoAge = ((binding.spnWeek.selectedItemPosition + 1) * 7) + binding.spnDays.selectedItemPosition
        val daysToAdd = pregnancyDays - embryoAge
        return calculateDate(daysToAdd)
    }

    private fun calculateDate(days: Int): Long {
        val calendar = Calendar.getInstance(Locale.ENGLISH)
        val dateSelected = binding.tvDate.text.toString()
        val date = DateUtils.dateFormatChange(DateFormat.MON_DATE_YEAR, DateFormat.DATE, dateSelected).toInt()
        val month = DateUtils.dateFormatChange(DateFormat.MON_DATE_YEAR, DateFormat.MONTH, dateSelected).toInt()
        val year = DateUtils.dateFormatChange(DateFormat.MON_DATE_YEAR, DateFormat.YEAR, dateSelected).toInt()
        calendar.set(year, (month - 1), date)

        calendar.add(Calendar.DAY_OF_MONTH, days)
        return calendar.timeInMillis
    }

    private fun setAdapter() {
        val list = resources.getStringArray(R.array.calculationMethod)
        itemsCalMethod.clear()
        list.forEachIndexed { index, s ->
            val item = Filter()
            if (index == 0)
                item.isSelected = true
            item.option_name = s
            itemsCalMethod.add(item)
        }

        adapterCalMethod = CheckItemAdapter(this, false, itemsCalMethod)
        binding.rvList.adapter = adapterCalMethod

        /*ivf list*/
        itemsIvf.clear()
        for (i in 0..1) {
            val item = Filter()
            if (i == 0) {
                item.isSelected = true
                item.option_name = "3 ${getString(R.string.days)}"
            } else if (i == 1) {
                item.option_name = "5 ${getString(R.string.days)}"
            }
            itemsIvf.add(item)
        }

        adapterIvf = CheckItemAdapter(this, false, itemsIvf)
        binding.rvListIVF.adapter = adapterIvf

        /*Cycle Length*/
        val itemCycle = ArrayList<String>()
        for (i in 20..35) {
            if (i == 20) itemCycle.add(getString(R.string.i_dont_know))
            else itemCycle.add("$i ${getString(R.string.days)}")
        }

        val adapterCycle = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, itemCycle)
        binding.spnCycleLength.adapter = adapterCycle

        /*Days*/
        val itemDays = ArrayList<String>()
        for (i in 0..6) {
            if (i == 1) itemDays.add("$i ${getString(R.string.day)}")
            else itemDays.add("$i ${getString(R.string.days)}")
        }

        val adapterDays = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, itemDays)
        binding.spnDays.adapter = adapterDays

        /*Weeks*/
        val itemWeeks = ArrayList<String>()
        for (i in 1..24) {
            if (i == 1) itemWeeks.add("$i ${getString(R.string.week)}")
            else itemWeeks.add("$i ${getString(R.string.weeks)}")
        }

        val adapterWeek = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, itemWeeks)
        binding.spnWeek.adapter = adapterWeek
    }


    fun itemRelationClick(pos: Int) {
        binding.rvListIVF.gone()
        binding.tvTitle2.gone()
        binding.tvCycle.gone()
        binding.tvWeek.gone()
        binding.tvDay.gone()

        binding.tvDate.text = ""
        binding.spnCycleLength.setSelection(0)
        binding.spnWeek.setSelection(0)
        binding.spnDays.setSelection(0)

        when (pos) {
            0 -> {
                binding.tvTitle2.visible()
                binding.tvCycle.visible()

                binding.tvTitleDate.text = getString(R.string.the_first_day_of_your_last_period)
                binding.tvTitle2.text = getString(R.string.cycle_length)

            }

            1 -> {
                binding.tvTitleDate.text = getString(R.string.date_of_conception)
            }

            2 -> {
                binding.tvTitle2.visible()
                binding.rvListIVF.visible()

                binding.tvTitleDate.text = getString(R.string.date_of_transfer)
                binding.tvTitle2.text = getString(R.string.ivf_transfer_date)

            }

            3 -> {
                binding.tvTitleDate.text = getString(R.string.date_of_ultrasound)
                binding.tvWeek.visible()
                binding.tvDay.visible()

            }
        }
    }

    override fun onDateSelected(date: String) {
        binding.tvDate.text = DateUtils.dateFormatChange(
            DateFormat.MON_DATE_YEAR,
            DateFormat.MON_DATE_YEAR, date
        )

    }
}