package org.kman.prefsx

import android.content.Intent
import android.content.res.XmlResourceParser
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable
import android.transition.TransitionManager
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.graphics.drawable.DrawerArrowDrawable
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import org.xmlpull.v1.XmlPullParser

abstract class PreferenceActivityX
	: AppCompatActivity(),
		PreferenceFragmentCompat.OnPreferenceDisplayDialogCallback {

	override fun onCreate(savedInstanceState: Bundle?) {
		mDefaultTitle = title

		super.onCreate(savedInstanceState)

		val res = resources
		val config = res.displayMetrics
		if (config.widthPixels >= res.getDimensionPixelSize(R.dimen.prefsx_use_two_panel_layout)) {
			mIsOnMultiPane = true
		}

		mRebuildHeaders = this::rebuildHeaders

		setContentView(
				if (mIsOnMultiPane) R.layout.prefsx_activity_two_panel
				else R.layout.prefsx_activity)

		val actionBar = requireNotNull(supportActionBar)
		actionBar.setIcon(DrawerArrowDrawable(actionBar.themedContext).apply {
			progress = 1.0f
		})
		actionBar.setDisplayHomeAsUpEnabled(true)

		mContentView = findViewById(R.id.prefsx_content)
		mHeaderListView = findViewById(R.id.prefsx_fragment_headers)
		mHeaderListAdapter = HeaderListAdapter(layoutInflater, this)

		mHeaderListView.apply {
			itemAnimator = DefaultItemAnimator()
			layoutManager = LinearLayoutManager(this@PreferenceActivityX)
			adapter = mHeaderListAdapter
		}

		val intent = intent
		mShowFragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
		mShowFragmentArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)

		if (savedInstanceState != null) {
			mIsLargeHeaderIcons = savedInstanceState.getBoolean(KEY_LARGE_HEADER_ICONS)
			title = savedInstanceState.getCharSequence(KEY_TITLE, mDefaultTitle)

			val targetList = savedInstanceState.getParcelableArrayList<Header>(KEY_HEADERS)
			if (targetList != null) {
				mTargetList.addAll(targetList)

				rebuildLargeHeaderIcons()

				mHeaderListAdapter.setHeaderList(mTargetList)
			}

			mCurrentFragment = savedInstanceState.getString(KEY_CURRENT_FRAGMENT)
			mCurrentFragmentArguments = savedInstanceState.getParcelable(KEY_CURRENT_FRAGMENT_ARGUMENTS)

			val currentFragment = mCurrentFragment
			if (currentFragment != null) {
				val currentHeader = findFragmentHeader(currentFragment, mCurrentFragmentArguments)
				if (mIsOnMultiPane) {
					mHeaderListAdapter.setActivatedItem(currentHeader)
				} else {
					mHeaderListView.visibility = View.GONE
				}
			}
		} else {
			mHandler.post(mRebuildHeaders)
		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> onBackPressed()
			else -> return super.onOptionsItemSelected(item)
		}
		return true
	}

	override fun onBackPressed() {
		val fm = supportFragmentManager

		super.onBackPressed()

		if (!isFinishing && fm.backStackEntryCount == 0) {
			if (mShowFragment != null) {
				finish()
			} else {
				TransitionManager.beginDelayedTransition(mContentView)
				if (mIsOnMultiPane) {
					mHeaderListAdapter.setActivatedItem(null)
				} else {
					mHeaderListView.visibility = View.VISIBLE
					title = mDefaultTitle
				}

				mCurrentFragment = null
				mCurrentFragmentArguments = null
			}
		}
	}

	override fun onSaveInstanceState(outState: Bundle) {
		super.onSaveInstanceState(outState)

		outState.putBoolean(KEY_LARGE_HEADER_ICONS, mIsLargeHeaderIcons)

		val fm = supportFragmentManager
		val prefsFragment = fm.findFragmentById(R.id.prefsx_fragment_preferences)
		if (prefsFragment != null) {
			outState.putCharSequence(KEY_TITLE, title)
		}

		outState.putParcelableArrayList(KEY_HEADERS, mTargetList)
		outState.putString(KEY_CURRENT_FRAGMENT, mCurrentFragment)
		outState.putParcelable(KEY_CURRENT_FRAGMENT_ARGUMENTS, mCurrentFragmentArguments)
	}

	override fun onPreferenceDisplayDialog(fragment: PreferenceFragmentCompat,
										   preference: Preference?): Boolean {
		val parent = fragment.parentFragmentManager
		if (parent.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
			return true
		}

		if (preference is DialogPreferenceX) {
			val dialogFragment = preference.createDialogFragment()
			if (dialogFragment != null) {
				dialogFragment.setTargetFragment(fragment, 0);
				dialogFragment.show(parent, DIALOG_FRAGMENT_TAG);
				return true
			}
		}

		return false
	}

	fun setUseLargeHeaderIcons(large: Boolean) {
		mIsLargeHeaderIcons = large
	}

	open fun getLargeHeaderIcon(iconRes: Int): Drawable? {
		return ContextCompat.getDrawable(this, iconRes)
	}

	class Header() : Parcelable {
		var title: String? = null
		var iconRes: Int = 0
		var fragment: String? = null
		var fragmentArguments: Bundle? = null
		var intent: Intent? = null
		var largeIcon: Drawable? = null

		constructor(parcel: Parcel) : this() {
			title = parcel.readString()
			iconRes = parcel.readInt()
			fragment = parcel.readString()
			fragmentArguments = parcel.readBundle(Bundle::class.java.classLoader)
			intent = parcel.readParcelable(Intent::class.java.classLoader)
		}

		override fun writeToParcel(parcel: Parcel, flags: Int) {
			parcel.writeString(title)
			parcel.writeInt(iconRes)
			parcel.writeString(fragment)
			parcel.writeBundle(fragmentArguments)
			parcel.writeParcelable(intent, flags)
		}

		override fun describeContents(): Int {
			return 0
		}

		companion object CREATOR : Parcelable.Creator<Header> {
			override fun createFromParcel(parcel: Parcel): Header {
				return Header(parcel)
			}

			override fun newArray(size: Int): Array<Header?> {
				return arrayOfNulls(size)
			}
		}
	}

	fun onIsMultiPane(): Boolean {
		return mIsOnMultiPane
	}

	fun invalidateHeaders() {
		mHandler.removeCallbacks(mRebuildHeaders)
		mHandler.post(mRebuildHeaders)
	}

	fun loadHeadersFromResource(resId: Int, target: MutableList<Header>) {
		resources.getXml(resId).use {
			loadHeadersFromResourceImpl(it, target)
		}
	}

	abstract fun onBuildHeaders(target: MutableList<Header>)

	open fun onGetNewHeader(): Header? {
		return mTargetList.firstOrNull()
	}

	private fun rebuildHeaders() {
		val oldActivated = mHeaderListAdapter.getActivatedItem()

		val target = ArrayList<Header>()
		onBuildHeaders(target)

		mTargetList.clear()
		mTargetList.addAll(target)

		rebuildLargeHeaderIcons()

		mHeaderListAdapter.setHeaderList(mTargetList)

		oldActivated?.fragment?.also {
			val newActivated = findFragmentHeader(it, oldActivated.fragmentArguments)
			mHeaderListAdapter.setActivatedItem(newActivated)
		}

		val showFragment = mShowFragment
		if (showFragment != null) {
			val header = findFragmentHeader(showFragment, mShowFragmentArguments)
			if (header != null) {
				switchToFragment(header)
			}
		} else if (mIsOnMultiPane) {
			val header = onGetNewHeader()
			if (header != null) {
				switchToFragment(header)
			}
		}
	}

	private fun rebuildLargeHeaderIcons() {
		if (mIsLargeHeaderIcons) {
			for (header in mTargetList) {
				if (header.iconRes != 0) {
					header.largeIcon = getLargeHeaderIcon(header.iconRes)
				}
			}
		}
	}

	private fun loadHeadersFromResourceImpl(xml: XmlResourceParser, target: MutableList<Header>) {
		while (true) {
			val event = xml.next()
			if (event == XmlPullParser.END_DOCUMENT) {
				break
			} else if (event == XmlPullParser.START_TAG) {
				if (xml.name == TAG_PREFERENCE_HEADERS) {
					parseHeadersList(xml, target)
				}
			} else if (event == XmlPullParser.END_TAG) {
				if (xml.name == TAG_PREFERENCE_HEADERS) {
					break
				}
			}
		}
	}

	private fun parseHeadersList(xml: XmlResourceParser, target: MutableList<Header>) {
		var currHeader: Header? = null

		while (true) {
			val event = xml.next()
			if (event == XmlPullParser.END_DOCUMENT) {
				break
			} else if (event == XmlPullParser.START_TAG) {
				when (xml.name) {
					TAG_HEADER -> {
						val a = obtainStyledAttributes(xml, R.styleable.PrefsXHeader)
						currHeader = Header().apply {
							fragment = a.getString(R.styleable.PrefsXHeader_android_fragment)
							title = a.getString(R.styleable.PrefsXHeader_android_title)
							iconRes = a.getResourceId(R.styleable.PrefsXHeader_android_icon, 0)
						}
						a.recycle()
					}
					TAG_INTENT -> {
						val a = obtainStyledAttributes(xml, R.styleable.PrefsXIntent)
						val intent = Intent().apply {
							setClassName(
									requireNotNull(a.getString(R.styleable.PrefsXIntent_android_targetPackage)),
									requireNotNull(a.getString(R.styleable.PrefsXIntent_android_targetClass)))
						}
						if (currHeader != null) {
							currHeader.intent = intent
						}
						a.recycle()
					}
				}
			} else if (event == XmlPullParser.END_TAG) {
				when (xml.name) {
					TAG_HEADER -> {
						if (currHeader != null) {
							if (currHeader.fragment != null || currHeader.intent != null) {
								target.add(currHeader)
							}
							currHeader = null
						}
					}
				}
			}
		}
	}

	private fun onHeaderClick(header: Header) {
		val intent = header.intent
		if (intent != null) {
			startActivity(intent)
		} else {
			switchToFragment(header)
		}
	}

	private fun switchToFragment(header: Header) {
		val fragment = header.fragment
		val args = header.fragmentArguments
		if (fragment != null) {
			if (mCurrentFragment == fragment &&
					mCurrentFragmentArguments == args) {
				return
			}

			val fm = supportFragmentManager
			fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)

			mCurrentFragment = fragment
			mCurrentFragmentArguments = args

			val factory = fm.fragmentFactory
			val prefsFragment = factory.instantiate(classLoader, fragment)
			if (args != null) {
				prefsFragment.arguments = args
			}

			fm.beginTransaction().apply {
				setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
				if (!mIsOnMultiPane) {
					addToBackStack(null)
				}
				replace(R.id.prefsx_fragment_preferences, prefsFragment)
			}.commitAllowingStateLoss()

			if (mIsOnMultiPane) {
				mHeaderListAdapter.setActivatedItem(header)
			} else {
				TransitionManager.beginDelayedTransition(mContentView)
				mHeaderListView.visibility = View.GONE
				title = header.title
			}
		}
	}

	private fun findFragmentHeader(fragment: String, fragmentArguments: Bundle?): Header? {
		val matches = ArrayList<Header>()
		for (item in mTargetList) {
			if (fragment == item.fragment) {
				matches.add(item)
			}
		}

		if (matches.isEmpty()) {
			return null
		}
		if (matches.size == 1) {
			return matches.first()
		}
		for (item in matches) {
			if (item.fragmentArguments == fragmentArguments) {
				return item
			}
		}

		return null
	}

	private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.prefs_header_image)
		val title: TextView = view.findViewById(R.id.prefs_header_title)
	}

	private class HeaderListAdapter(val inflater: LayoutInflater,
									val activity: PreferenceActivityX)
		: RecyclerView.Adapter<HeaderViewHolder>() {
		val list = ArrayList<Header>()
		var activated: Header? = null

		fun setHeaderList(target: List<Header>) {
			list.clear()
			list.addAll(target)
			notifyDataSetChanged()
		}

		fun getActivatedItem(): Header? {
			return activated
		}

		fun setActivatedItem(item: Header?) {
			if (activated != item) {
				for (i in list.indices) {
					val listItem = list.get(i)
					if (listItem == activated || listItem == item) {
						notifyItemChanged(i)
					}
				}
				activated = item
			}
		}

		override fun getItemCount(): Int {
			return list.size
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
			val layoutId =
					if (activity.mIsLargeHeaderIcons) R.layout.prefsx_header_large_icon
					else R.layout.prefsx_header
			val view = inflater.inflate(layoutId, parent, false)
			return HeaderViewHolder(view).apply {
				view.setOnClickListener(this@HeaderListAdapter::onHeaderClick)
			}
		}

		override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
			val item = list.get(position)
			if (item.iconRes != 0) {
				if (activity.mIsLargeHeaderIcons) {
					holder.image.setImageDrawable(item.largeIcon)
				} else {
					holder.image.setImageResource(item.iconRes)
				}
				holder.image.visibility = View.VISIBLE
				holder.image.contentDescription = item.title
			} else {
				holder.image.visibility = View.GONE
			}

			holder.title.text = item.title
			holder.itemView.isActivated = item == activated
			holder.itemView.tag = item
		}

		private fun onHeaderClick(view: View) {
			val item = view.tag as Header
			activity.onHeaderClick(item)
		}
	}

	companion object {
		const val EXTRA_SHOW_FRAGMENT = "_show_fragment"
		const val EXTRA_SHOW_FRAGMENT_ARGUMENTS = "_show_fragment_arguments"

		private const val TAG_PREFERENCE_HEADERS = "preference-headers"
		private const val TAG_HEADER = "header"
		private const val TAG_INTENT = "intent"

		private const val DIALOG_FRAGMENT_TAG = "_dialog_fragment"

		private const val KEY_LARGE_HEADER_ICONS = "large_header_icons"
		private const val KEY_TITLE = "title"
		private const val KEY_HEADERS = "headers"
		private const val KEY_CURRENT_FRAGMENT = "current_fragment"
		private const val KEY_CURRENT_FRAGMENT_ARGUMENTS = "current_fragment_arguments"
	}

	private var mDefaultTitle: CharSequence? = null
	private var mIsOnMultiPane = false

	private var mShowFragment: String? = null
	private var mShowFragmentArguments: Bundle? = null

	private var mCurrentFragment: String? = null
	private var mCurrentFragmentArguments: Bundle? = null

	private val mTargetList = ArrayList<Header>()

	private val mHandler = Handler()
	private lateinit var mRebuildHeaders: () -> Unit

	private var mIsLargeHeaderIcons = false

	private lateinit var mContentView: ViewGroup
	private lateinit var mHeaderListView: RecyclerView
	private lateinit var mHeaderListAdapter: HeaderListAdapter
}