package org.kman.prefsx

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import androidx.preference.*

abstract class PreferenceFragmentX : PreferenceFragmentCompat() {

	override fun onAttach(context: Context) {
		super.onAttach(context)

		mCollapseIconSpaceReserved =
				context.resources.getBoolean(R.bool.prefsx_collapse_icon_space_reserved)
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)

		if (savedInstanceState != null) {
			mRingtonePickKey = savedInstanceState.getString(SAVE_STATE_RINGTONE_PICK_KEY)
		}
	}

	override fun setPreferenceScreen(preferenceScreen: PreferenceScreen?) {
		super.setPreferenceScreen(preferenceScreen)

		if (mCollapseIconSpaceReserved && preferenceScreen != null) {
			val queue = java.util.ArrayDeque<PreferenceGroup>(0)
			queue.add(preferenceScreen)

			while (queue.isNotEmpty()) {
				val group = queue.remove()
				val count = group.preferenceCount
				for (i in 0 until count) {
					val pref = group.getPreference(i)
					pref?.isIconSpaceReserved = false
					if (pref is PreferenceCategory) {
						queue.add(pref)
					}
				}
			}
		}
	}

	fun setCollapseIconSpaceReserved(collapse: Boolean) {
		mCollapseIconSpaceReserved = collapse
	}

	override fun onPreferenceTreeClick(preference: Preference?): Boolean {
		if (preference is RingtonePreferenceX) {
			val key = preference.key
			if (key != null) {
				mRingtonePickKey = key
				preference.onPreferenceClick(this, REQ_CODE_RINGTONE)
				return true
			}
		}

		return super.onPreferenceTreeClick(preference)
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		val key = mRingtonePickKey
		if (key != null) {
			outState.putString(SAVE_STATE_RINGTONE_PICK_KEY, key)
		}
	}

	override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
		if (requestCode == REQ_CODE_RINGTONE) {
			if (resultCode == Activity.RESULT_OK && data != null) {
				val key = mRingtonePickKey
				if (key != null) {
					val pref = findPreference<Preference>(key)
					if (pref is RingtonePreferenceX) {
						val uri: Uri? = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
						pref.onActivityResult(uri)
					}
				}
			}

			return
		}

		super.onActivityResult(requestCode, resultCode, data)
	}

	companion object {
		private const val REQ_CODE_RINGTONE = 1

		private const val SAVE_STATE_RINGTONE_PICK_KEY = "ringtone_pick_key"
	}

	private var mRingtonePickKey: String? = null
	private var mCollapseIconSpaceReserved = true
}