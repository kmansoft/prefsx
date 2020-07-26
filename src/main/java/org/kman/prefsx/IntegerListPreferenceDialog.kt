package org.kman.prefsx

import android.content.DialogInterface
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat

class IntegerListPreferenceDialog : PreferenceDialogFragmentCompat() {

	private var mClickedDialogEntryIndex = 0
	private var mEntries: Array<CharSequence>? = null
	private var mEntryValues: IntArray? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		if (savedInstanceState == null) {
			val preference = getListPreference()
			mClickedDialogEntryIndex = preference.findIndexOfValue(preference.getValue())
			mEntries = requireNotNull(preference.getEntries())
			mEntryValues = requireNotNull(preference.getEntryValues())
		} else {
			mClickedDialogEntryIndex = savedInstanceState.getInt(SAVE_STATE_INDEX, 0)
			mEntries = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRIES)
			mEntryValues = savedInstanceState.getIntArray(SAVE_STATE_ENTRY_VALUES)
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)
		outState.putInt(SAVE_STATE_INDEX, mClickedDialogEntryIndex)
		outState.putCharSequenceArray(SAVE_STATE_ENTRIES, mEntries)
		outState.putIntArray(SAVE_STATE_ENTRY_VALUES, mEntryValues)
	}

	override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
		super.onPrepareDialogBuilder(builder)
		builder.setSingleChoiceItems(mEntries, mClickedDialogEntryIndex) { dialog, which ->
			mClickedDialogEntryIndex = which

			this@IntegerListPreferenceDialog.onClick(dialog, DialogInterface.BUTTON_POSITIVE)
			dialog.dismiss()
		}

		// The typical interaction for list-based dialogs is to have click-on-an-item dismiss the
		// dialog instead of the user having to press 'Ok'.
		builder.setPositiveButton(null, null)
	}

	override fun onDialogClosed(positiveResult: Boolean) {
		if (positiveResult && mClickedDialogEntryIndex >= 0) {
			val value = requireNotNull(mEntryValues)[mClickedDialogEntryIndex]
			val preference = getListPreference()
			if (preference.callChangeListener(value)) {
				preference.setValue(value)
			}
		}
	}

	private fun getListPreference(): IntegerListPreference {
		return preference as IntegerListPreference
	}

	companion object {
		private const val SAVE_STATE_INDEX = "IntegerListPreferenceDialogFragment.index"
		private const val SAVE_STATE_ENTRIES = "IntegerListPreferenceDialogFragment.entries"
		private const val SAVE_STATE_ENTRY_VALUES = "IntegerListPreferenceDialogFragment.entryValues"

		fun newInstance(key: String?): IntegerListPreferenceDialog {
			val fragment = IntegerListPreferenceDialog()
			val b = Bundle(1)
			b.putString(ARG_KEY, key)
			fragment.arguments = b
			return fragment
		}
	}
}