package org.kman.prefsx

import android.content.Context
import android.util.AttributeSet
import androidx.fragment.app.DialogFragment
import androidx.preference.DialogPreference
import androidx.preference.R

abstract class DialogPreferenceX(
	context: Context,
	attrs: AttributeSet?,
	defStyleAttr: Int,
	defStyleRes: Int
) : DialogPreference(context, attrs, defStyleAttr, defStyleRes) {

	constructor(
		context: Context,
		attrs: AttributeSet?,
		defStyleAttr: Int
	) : this(context, attrs, defStyleAttr, 0) {
	}

	constructor(
		context: Context,
		attrs: AttributeSet?
	) : this(
		context, attrs, TypedArrayUtils.getAttr(
			context, R.attr.dialogPreferenceStyle,
			android.R.attr.dialogPreferenceStyle
		), 0
	) {
	}

	abstract fun createDialogFragment(): DialogFragment?
}