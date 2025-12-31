package org.kman.prefsx

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.preference.PreferenceDialogFragmentCompat

class IntegerMaskPreferenceDialog : PreferenceDialogFragmentCompat() {
    private var mNewValue = 0
    private var mEntryList: Array<CharSequence>? = null
    private var mValueList: IntArray? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            val preference = getListPreference()
            mNewValue = preference.getValue()
            mEntryList = requireNotNull(preference.getEntryList())
            mValueList = requireNotNull(preference.getValueList())
        } else {
            mNewValue = savedInstanceState.getInt(SAVE_STATE_NEW_VALUE)
            mEntryList = savedInstanceState.getCharSequenceArray(SAVE_STATE_ENTRY_LIST)
            mValueList = savedInstanceState.getIntArray(SAVE_STATE_VALUE_LIST)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(SAVE_STATE_NEW_VALUE, mNewValue)
        outState.putCharSequenceArray(SAVE_STATE_ENTRY_LIST, mEntryList)
        outState.putIntArray(SAVE_STATE_VALUE_LIST, mValueList)
    }

    override fun onPrepareDialogBuilder(builder: AlertDialog.Builder) {
        super.onPrepareDialogBuilder(builder)

        val preference = getListPreference()
        mNewValue = preference.getValue()

        val entryList = requireNotNull(mEntryList)
        val valueList = requireNotNull(mValueList)

        val checked = BooleanArray(valueList.size) { (mNewValue and valueList[it]) != 0 }
        builder.setMultiChoiceItems(entryList, checked) { _, which, isChecked ->
            if (which in valueList.indices) {
                val bit = valueList[which]
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
        private const val SAVE_STATE_ENTRY_LIST = "IntegerMaskPreferenceDialogFragment.entry_list"
        private const val SAVE_STATE_VALUE_LIST =
            "IntegerMaskPreferenceDialogFragment.value_list"

        fun newInstance(key: String?): IntegerMaskPreferenceDialog {
            val fragment = IntegerMaskPreferenceDialog()
            val b = Bundle(1)
            b.putString(ARG_KEY, key)
            fragment.arguments = b
            return fragment
        }
    }
}