package org.kman.prefsx

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.fragment.app.DialogFragment

class IntegerMaskPreference(context: Context, attrs: AttributeSet)
	: DialogPreferenceX(context, attrs) {
	private var mValueList: IntArray
	private var mEntryList: Array<CharSequence>
	private var mValue = -1
	private var mValueSet = false

	init {
		val res = context.resources
		val a = context.obtainStyledAttributes(attrs, R.styleable.IntegerListPreference)
		val valueListId = a.getResourceId(R.styleable.IntegerListPreference_valueList, 0)
		mValueList = res.getIntArray(valueListId)
		mEntryList = a.getTextArray(R.styleable.IntegerListPreference_entryList)
		a.recycle()
	}

	fun getValue(): Int {
		return mValue
	}

	fun getEntries(): Array<CharSequence>? {
		return mEntryList
	}

	fun getEntryValues(): IntArray? {
		return mValueList
	}

	fun setValue(value: Int) {
		if (mValue != value || !mValueSet) {
			mValue = value
			mValueSet = true
			persistInt(value)
			updateSummary()
			notifyChanged()
		}
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
		return a.getInt(index, 0)
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		setValue(
				getPersistedInt(
						if (defaultValue is Int) defaultValue else 0))
	}

	override fun createDialogFragment(): DialogFragment? {
		return IntegerMaskPreferenceDialog.newInstance(key)
	}

	private fun updateSummary() {
		summary = if (mValue == 0) {
			null
		} else {
			val sb = StringBuilder()
			for (i in mValueList.indices) {
				if ((mValue and mValueList[i]) != 0) {
					if (sb.isNotEmpty()) {
						sb.append(", ")
					}
					sb.append(mEntryList[i])
				}
			}
			sb.toString()
		}
	}
}
