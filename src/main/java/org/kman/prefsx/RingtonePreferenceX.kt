package org.kman.prefsx

import android.content.Context
import android.content.Intent
import android.content.res.TypedArray
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.util.AttributeSet
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.preference.Preference

class RingtonePreferenceX(context: Context, attrs: AttributeSet)
	: Preference(context, attrs) {

	// content://settings/system/notification_sound

	private var mRingtoneType: Int
	private var mShowDefault: Boolean
	private var mShowSilent: Boolean

	init {
		val a = context.obtainStyledAttributes(attrs, R.styleable.RingtonePreferenceX)

		mRingtoneType = a.getInt(R.styleable.RingtonePreferenceX_android_ringtoneType,
				RingtoneManager.TYPE_NOTIFICATION)
		mShowDefault = a.getBoolean(R.styleable.RingtonePreferenceX_android_showDefault, true)
		mShowSilent = a.getBoolean(R.styleable.RingtonePreferenceX_android_showSilent, true)

		a.recycle()
    }

	fun getRingtoneType(): Int {
		return mRingtoneType
	}

	fun setRingtoneType(type: Int) {
		mRingtoneType = type
	}

	fun getShowDefault(): Boolean {
		return mShowDefault
	}

	fun setShowDefault(showDefault: Boolean) {
		mShowDefault = showDefault
	}

	fun getShowSilent(): Boolean {
		return mShowSilent
	}

	fun setShowSilent(showSilent: Boolean) {
		mShowSilent = showSilent
	}

	fun getValue(): String? {
		return mValue
	}

	fun setValue(value: String?) {
		if (mValue != value || !mIsValueSet) {
			mValue = value
			persistString(value)
			updateSummary()
		}
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Any? {
		return a.getString(index)
	}

	override fun onSetInitialValue(defaultValue: Any?) {
		setValue(
				getPersistedString(
						if (defaultValue is String) defaultValue else null))
	}

	internal fun onPreferenceClick(request: ActivityResultLauncher<Intent>) {
		// Launch the ringtone picker
		val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER)
		onPrepareRingtonePickerIntent(intent)

		request.launch(intent)
	}

	internal fun onActivityResult(uri: Uri?) {
		val s = uri?.toString()
		if (callChangeListener(s)) {
			setValue(s)
		}
	}

	private fun onPrepareRingtonePickerIntent(ringtonePickerIntent: Intent): Unit {
		val value = mValue
		if (value != null) {
			ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
					Uri.parse(value))
		}
		ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, mShowDefault)
		if (mShowDefault) {
			ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
					RingtoneManager.getDefaultUri(getRingtoneType()))
		}
		ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, mShowSilent)
		ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, mRingtoneType)
		ringtonePickerIntent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, title)
		ringtonePickerIntent.putExtra(EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS,
				FLAG_BYPASS_INTERRUPTION_POLICY)
	}

	private fun updateSummary() {
		val context = context
		val value = mValue
		if (value == null) {
			summary = context.getString(R.string.prefsx_notify_sound_none)
		} else {
			val ringtone = RingtoneManager.getRingtone(context, Uri.parse(value))
			if (ringtone != null) {
				val title = ringtone.getTitle(context)
				summary = title
			}
		}
	}

	companion object {
		// @hide in RingtoneManager
		private const val EXTRA_RINGTONE_AUDIO_ATTRIBUTES_FLAGS =
				"android.intent.extra.ringtone.AUDIO_ATTRIBUTES_FLAGS"

		// @SystemAPI in AudioAutributes
		private const val FLAG_BYPASS_INTERRUPTION_POLICY: Int = 0x1 shl 6
	}

	private var mValue: String? = null
	private val mIsValueSet = false
}
