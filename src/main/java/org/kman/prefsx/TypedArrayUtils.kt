package org.kman.prefsx

import android.content.Context
import android.util.TypedValue

object TypedArrayUtils {
	fun getAttr(context: Context, attr: Int, fallbackAttr: Int): Int {
		val value = TypedValue()
		context.theme.resolveAttribute(attr, value, true)
		return if (value.resourceId != 0) {
			attr
		} else fallbackAttr
	}

}