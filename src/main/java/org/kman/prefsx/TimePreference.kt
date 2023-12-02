package org.kman.prefsx

import android.content.Context
import android.content.res.TypedArray
import android.text.format.DateFormat
import android.util.AttributeSet
import android.view.View
import android.widget.TimePicker
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference
import org.kman.prefsx.DialogPreferenceX
import java.util.*

class TimePreference(context: Context, attrs: AttributeSet)
	: DialogPreferenceX(context, attrs) {

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any {
		return a.getInt(index, -1)
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		setValue(
				getPersistedInt(
						if (defaultValue is Int) defaultValue else 0))
	}

	override fun createDialogFragment(): DialogFragment {
		return TimePreferenceDialog.newInstance(key)
	}

	fun getValue(): Int {
		return mValue
	}

	fun setValue(value: Int) {
		if (mValue != value || !mIsValueSet) {
			mValue = value
			mIsValueSet = true

			persistInt(value)
			updateSummary()
			notifyChanged()
		}
	}

	private fun updateSummary() {
		val context = context
		summary = if (mValue == -1) {
			null
		} else {
			val hours = mValue / 100
			val minutes = mValue % 100
			val cal = Calendar.getInstance()
			cal.set(Calendar.HOUR_OF_DAY, hours)
			cal.set(Calendar.MINUTE, minutes)

			DateFormat.getTimeFormat(context).format(Date(cal.timeInMillis))
		}
	}

	private var mValue = -1
	private var mIsValueSet = false
}
