package com.consultantvendor.utils

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Context
import android.text.format.DateUtils
import androidx.fragment.app.FragmentActivity
import com.consultantvendor.R
import com.consultantvendor.ui.loginSignUp.availability.OnTimeSelected
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.DateValidatorPointBackward
import com.google.android.material.datepicker.DateValidatorPointForward
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*


object DateUtils {

    val utcFormat = SimpleDateFormat(DateFormat.UTC_FORMAT_NORMAL, Locale.ENGLISH)

    fun openDatePicker(activity: Activity, listener: OnDateSelected, max: Long?, min: Long?) {
        val constraintsBuilder = CalendarConstraints.Builder()
        when {
            max != null && min != null -> {
                constraintsBuilder.setEnd(max)
                constraintsBuilder.setStart(min)
                constraintsBuilder.setValidator(DateValidatorPointForward.from(min))
            }
            max != null -> {
                constraintsBuilder.setEnd(max)
                constraintsBuilder.setValidator(DateValidatorPointBackward.before(max))
            }
            min != null -> {
                constraintsBuilder.setStart(min)
                constraintsBuilder.setValidator(DateValidatorPointForward.from(min))
            }
        }

        val defaultSelection = when {
            max != null -> max - 86400000L
            min != null -> min + 86400000L
            else -> System.currentTimeMillis()
        }

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(activity.getString(R.string.select_date))
            .setSelection(defaultSelection)
            .setCalendarConstraints(constraintsBuilder.build())
            .build()

        picker.addOnPositiveButtonClickListener { selection ->
            val fmt = SimpleDateFormat(DateFormat.DATE_FORMAT, Locale.ENGLISH)
            fmt.timeZone = TimeZone.getTimeZone("UTC")
            var selectedDate = fmt.format(selection)
            selectedDate = dateFormatChange(DateFormat.DATE_FORMAT, DateFormat.MON_DATE_YEAR, selectedDate)
            listener.onDateSelected(selectedDate)
        }

        (activity as? FragmentActivity)?.supportFragmentManager?.let {
            picker.show(it, "date_picker")
        }
    }

    fun openDatePickerDialog(activity: Activity, listener: OnDateSelected, max: Long?, min: Long?) {
        val c = Calendar.getInstance()
        val year = c.get(Calendar.YEAR)
        val month = c.get(Calendar.MONTH)
        val day = c.get(Calendar.DAY_OF_MONTH)

        val dpd = DatePickerDialog(
            activity, { _, y, monthOfYear, dayOfMonth ->
                var selectedDate = "$dayOfMonth/${monthOfYear.plus(1)}/$y"
                selectedDate = dateFormatChange(
                    DateFormat.DATE_FORMAT_SLASH_YEAR,
                    DateFormat.MON_DATE_YEAR,
                    selectedDate
                )
                listener.onDateSelected(selectedDate)
            }, year, month, day
        )

        if (max != null) dpd.datePicker.maxDate = max
        if (min != null) dpd.datePicker.minDate = min

        dpd.show()
    }

    fun getTime(
        context: Context, startTime: String = "", endTime: String? = null,
        isStart: Boolean = true, listener: OnTimeSelected
    ) {
        var compareDate = true
        if (endTime == null) compareDate = false
        else if (isStart && endTime.isEmpty()) compareDate = false
        else if (startTime.isEmpty()) compareDate = false

        val sdf = SimpleDateFormat(DateFormat.TIME_FORMAT_24, Locale.ENGLISH)
        val parsedEnd = if (endTime != null && endTime.isNotEmpty()) sdf.parse(endTime) else Date()
        val parsedStart = if (startTime.isNotEmpty()) sdf.parse(startTime) else Date()

        val now = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(now.get(Calendar.HOUR_OF_DAY))
            .setMinute(now.get(Calendar.MINUTE))
            .setTitleText(context.getString(R.string.select_time))
            .build()

        picker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.SECOND, 0)

            val newTime = sdf.parse(sdf.format(cal.time))!!
            val time = sdf.format(cal.time)
            var selectedTime = ""
            var isError = false

            if (isStart) {
                if (!compareDate || newTime.before(parsedEnd)) selectedTime = time
                else isError = true
            } else {
                if (!compareDate || parsedStart!!.before(newTime)) selectedTime = time
                else isError = true
            }

            listener.onTimeSelected(Triple(selectedTime, isStart, isError))
        }

        (context as? FragmentActivity)?.supportFragmentManager?.let {
            picker.show(it, "time_picker")
        }
    }

    fun getTime1(
        context: Context, startTime: String = "", endTime: String? = null,
        isStart: Boolean = true, listener: OnTimeSelected
    ) {
        var compareDate = true
        if (endTime == null) compareDate = false
        else if (isStart && endTime.isEmpty()) compareDate = false
        else if (startTime.isEmpty()) compareDate = false

        val sdf = SimpleDateFormat(DateFormat.TIME_FORMAT, Locale.ENGLISH)
        val parsedEnd = if (endTime != null && endTime.isNotEmpty()) sdf.parse(endTime) else Date()
        val parsedStart = if (startTime.isNotEmpty()) sdf.parse(startTime) else Date()

        val now = Calendar.getInstance()
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(now.get(Calendar.HOUR_OF_DAY))
            .setMinute(now.get(Calendar.MINUTE))
            .setTitleText(context.getString(R.string.select_time))
            .build()

        picker.addOnPositiveButtonClickListener {
            val cal = Calendar.getInstance()
            cal.set(Calendar.HOUR_OF_DAY, picker.hour)
            cal.set(Calendar.MINUTE, picker.minute)
            cal.set(Calendar.SECOND, 0)

            // mustBeOnFuture equivalent — reject times in the past
            if (cal.timeInMillis <= System.currentTimeMillis()) {
                context.longToast(context.getString(R.string.select_future_date))
                listener.onTimeSelected(Triple("", isStart, true))
                return@addOnPositiveButtonClickListener
            }

            val newTime = sdf.parse(sdf.format(cal.time))!!
            val time = sdf.format(cal.time)
            var selectedTime = ""
            var isError = false

            if (isStart) {
                if (!compareDate || newTime.before(parsedEnd)) selectedTime = time
                else isError = true
            } else {
                if (!compareDate || parsedStart!!.before(newTime)) selectedTime = time
                else isError = true
            }

            listener.onTimeSelected(Triple(selectedTime, isStart, isError))
        }

        (context as? FragmentActivity)?.supportFragmentManager?.let {
            picker.show(it, "time_picker_1")
        }
    }

    fun dateFormatFromMillis(format: String, timeInMillis: Long?): String {
        val fmt = SimpleDateFormat(format, Locale.ENGLISH)
        return if (timeInMillis == null || timeInMillis == 0L) ""
        else fmt.format(timeInMillis)
    }

    fun dateFormatFromMillisBackend(format: String, timeInMillis: Long?): String {
        val fmt = SimpleDateFormat(format, Locale.ENGLISH)
        return if (timeInMillis == null || timeInMillis == 0L) ""
        else fmt.format(timeInMillis)
    }

    fun dateFormatChange(formatFrom: String, formatTo: String, value: String): String {
        val originalFormat = SimpleDateFormat(formatFrom, Locale.ENGLISH)
        val targetFormat = SimpleDateFormat(formatTo, Locale.ENGLISH)
        val date = try {
            originalFormat.parse(value)
        } catch (e: Exception) {
            originalFormat.parse(value)
        }
        return targetFormat.format(date)
    }

    fun dateFormatForBackend(formatFrom: String, formatTo: String, value: String): String {
        val originalFormat = SimpleDateFormat(formatFrom, Locale.ENGLISH)
        val targetFormat = SimpleDateFormat(formatTo, Locale.ENGLISH)
        val date = try {
            originalFormat.parse(value)
        } catch (e: Exception) {
            originalFormat.parse(value)
        }
        return targetFormat.format(date)
    }

    fun localToUTC(dateFormat: String, datesToConvert: String): String {
        var dateToReturn = datesToConvert
        val sdf = SimpleDateFormat(dateFormat)
        sdf.timeZone = TimeZone.getDefault()
        val sdfOut = SimpleDateFormat(DateFormat.DATE_FORMAT)
        sdfOut.timeZone = TimeZone.getTimeZone("UTC")
        try {
            val gmt = sdf.parse(datesToConvert)
            dateToReturn = sdfOut.format(gmt)
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        return dateToReturn
    }

    fun getTimeAgo(createdAt: String?): String {
        if (createdAt == null) return ""
        utcFormat.timeZone = TimeZone.getTimeZone("Etc/UTC")
        val time = utcFormat.parse(createdAt).time
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(
            time, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    fun getTimeAgoForMillis(millis: Long): String {
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(
            millis, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    fun getLocalTimeAgo(timeString: Long?, removeAgo: String): String {
        var agoString = ""
        timeString?.let {
            val now = System.currentTimeMillis()
            val ago = DateUtils.getRelativeTimeSpanString(
                timeString, now, DateUtils.SECOND_IN_MILLIS, DateUtils.FORMAT_SHOW_TIME
            )
            agoString = ago.toString()
        }
        return agoString
    }

    fun dateTimeFormatFromUTC(format: String, createdDate: String?): String {
        return if (createdDate == null || createdDate.isEmpty()) ""
        else {
            utcFormat.timeZone = TimeZone.getTimeZone("Etc/UTC")
            val fmt = SimpleDateFormat(format, Locale.ENGLISH)
            fmt.format(utcFormat.parse(createdDate))
        }
    }
}

/*On Date selected listener*/
interface OnDateSelected {
    fun onDateSelected(date: String)
}

fun isYesterday(calendar: Calendar): Boolean {
    val tempCal = Calendar.getInstance()
    tempCal.add(Calendar.DAY_OF_MONTH, -1)
    return calendar.get(Calendar.DAY_OF_MONTH) == tempCal.get(Calendar.DAY_OF_MONTH)
}

object DateFormat {
    const val DATE_FORMAT = "yyyy-MM-dd"
    const val DATE_TIME_FORMAT = "MMM dd, yyyy · hh:mm a"
    const val TIME_FORMAT = "hh:mm a"
    const val TIME_FORMAT_24 = "HH:mm"
    const val MON_DATE = "MMM dd"
    const val MON_DATE_YEAR = "MMM dd, yyyy"
    const val DATE_FORMAT_SLASH_YEAR = "dd/MM/yyyy"
    const val UTC_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    const val UTC_FORMAT_NORMAL = "yyyy-MM-dd HH:mm:ss"

    const val DATE = "dd"
    const val MONTH = "MM"
    const val YEAR = "yyyy"
    const val HH_24 = "HH"
    const val MM = "mm"
}
