package org.kman.prefsx

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference

abstract class DialogPreferenceX(context: Context, attrs: AttributeSet?)
	: DialogPreference(context, attrs) {

	abstract fun createDialogFragment() : DialogFragment?
}