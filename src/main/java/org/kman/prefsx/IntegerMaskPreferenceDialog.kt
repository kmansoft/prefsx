package org.kman.prefsx

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat

class IntegerMaskPreferenceDialog : PreferenceDialogFragmentCompat() {
	private var mNewValue = 0
	private var mEntries: Array<CharSequence>? = null
	private var mEntryValues: IntArray? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			val preference = getListPreference()
			mNewValue = preference.getValue()
			mEntries = requireNotNull(preference.getEntries())
			mEntryValues = requireNotNull(preference.getEntryValues())
		} else {
			mNewValue = savedInstanceState.getInt(SAVE_STATE_NEW_VALUE)
			mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
			mEntryValues = savedInstanceState.getIntArray(SAVE_STATE_ENTRY_VALUES)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(SAVE_STATE_NEW_VALUE, mNewValue)
		outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
		outState.putIntArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
	}

	override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
		super.onPrepareDialogBuilder(builder)

		val preference = getListPreference()
		mNewValue = preference.getValue()

		val entries = requireNotNull(mEntries)
		val entryValues = requireNotNull(mEntryValues)

		val checked = BooleanArray(entryValues.size) { (mNewValue and entryValues[it]) != 0 }
		builder.setMultiChoiceItems(entries, checked) { _, which, isChecked ->
			if (which in entryValues.indices) {
				val bit = entryValues[which]
				mNewValue = if (isChecked) {
					mNewValue or bit
				} else {
					mNewValue and bit.inv()
				}
			}
		}
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult) {
			val preference = getListPreference()
			if (preference.callChangeListener(mNewValue)) {
				preference.setValue(mNewValue)
			}
		}
	}

	private fun getListPreference(): IntegerMaskPreference {
		return preference as IntegerMaskPreference
	}

	companion object {
		private const val SAVE_STATE_NEW_VALUE = "IntegerMaskPreferenceDialogFragment.new_value"
		private const val SAVE_STATE_ENTRIES = "IntegerMaskPreferenceDialogFragment.entries"
		private const val SAVE_STATE_ENTRY_VALUES = "IntegerMaskPreferenceDialogFragment.entryValues"

		fun newInstance(key: String?): IntegerMaskPreferenceDialog {
			val fragment = IntegerMaskPreferenceDialog()
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}