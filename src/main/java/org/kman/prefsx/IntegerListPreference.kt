package org.kman.prefsx

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import androidx.fragment.app.DialogFragment

class IntegerListPreference(context: Context, attrs: AttributeSet)
	: DialogPreferenceX(context, attrs) {
	private var mValueList: IntArray
	private var mEntryList: Array<CharSequence>
	private var mValue = -1
	private var mValueSet = false
	private var mChangedListener: ChangedListener? = null
	private var mDisableDependentsValue = -1

	interface ChangedListener {
		fun onChanged(newValue: Int)
	}

	init {
		val res = context.resources
		val a = context.obtainStyledAttributes(attrs, R.styleable.IntegerListPreference)

		val valueListId = a.getResourceId(R.styleable.IntegerListPreference_valueList, 0)
		mValueList = res.getIntArray(valueListId)

		val entryTemplateId = a.getResourceId(R.styleable.IntegerListPreference_entryTemplate, 0)
		mEntryList =
				if (entryTemplateId != 0) Array(mValueList.size) {
					res.getString(entryTemplateId, mValueList[it])
				} else a.getTextArray(R.styleable.IntegerListPreference_entryList)

		mDisableDependentsValue = a.getInt(R.styleable.IntegerListPreference_disableDependentsValue, -1)

		a.recycle()
	}

	override fun shouldDisableDependents(): Boolean {
		return super.shouldDisableDependents() ||
				mValueSet && mValue == mDisableDependentsValue
	}

	fun setValueAndEntryList(valueList: IntArray, entryList: Array<CharSequence>) {
		mValueList = valueList.copyOf()
		mEntryList = entryList.copyOf()
		updateSummary()
	}

	fun getValue(): Int {
		return mValue
	}

	fun setValue(value: Int) {
		if (mValue != value || !mValueSet) {
			mValue = value
			mValueSet = true

			persistInt(value)
			updateSummary()
			notifyChanged()
			notifyDependencyChange(shouldDisableDependents())

			mChangedListener?.onChanged(value)
		}
	}

	fun getEntries(): Array<CharSequence>? {
		return mEntryList
	}

	fun getEntryValues(): IntArray? {
		return mValueList
	}

	fun findIndexOfValue(value: Int): Int {
		for (i in mValueList.indices) {
			if (mValueList[i] == value) {
				return i
			}
		}
		return -1
	}

	fun setChangedListener(listener: ChangedListener) {
		mChangedListener = listener
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
		return IntegerListPreferenceDialog.newInstance(key)
	}

	private fun updateSummary() {
		val index = indexOfValue(mValue)
		summary = if (index >= 0) {
			mEntryList[index]
		} else {
			null
		}
	}

	private fun indexOfValue(value: Int): Int {
		for (i in mValueList.indices) {
			if (mValueList[i] == value) {
				return i
			}
		}
		return -1
	}
}
