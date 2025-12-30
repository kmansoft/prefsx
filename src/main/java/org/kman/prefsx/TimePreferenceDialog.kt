package org.kman.prefsx

import android.content.Context
import android.os.Bundle
import android.text.format.DateFormat
import android.view.View
import android.widget.TimePicker
import androidx.preference.PreferenceDialogFragmentCompat

class TimePreferenceDialog : PreferenceDialogFragmentCompat() {

	@Suppress("DEPRECATION")
	override fun onCreateDialogView(context: Context): View {
		val preference: TimePreference = getTimePreference()
		val value = preference.getValue()
		val picker = TimePicker(context)

        picker.setIs24HourView(DateFormat.is24HourFormat(context))

        if (value == -1) {
			picker.currentHour = 0
			picker.currentMinute = 0
		} else {
			val hours = value / 100
			val minutes = value % 100
			picker.currentHour = hours
			picker.currentMinute = minutes
		}

		mTimePicker = picker
		return picker
	}

	@Suppress("DEPRECATION")
	override fun onDialogClosed(positiveResult: Boolean) {
		val picker = mTimePicker
		mTimePicker = null

		if (positiveResult && picker != null) {
			val hours = picker.currentHour
			val minutes = picker.currentMinute

			val value = hours * 100 + minutes
			val preference: TimePreference = getTimePreference()
			if (preference.callChangeListener(value)) {
				preference.setValue(value)
			}
		}
	}

	private fun getTimePreference(): TimePreference {
		return preference as TimePreference
	}

	private var mTimePicker: TimePicker? = null

	companion object {
		fun newInstance(key: String?): TimePreferenceDialog {
			val fragment = TimePreferenceDialog()
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}