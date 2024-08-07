package org.kman.prefsx

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.res.XmlResourceParser
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.*
import android.transition.TransitionManager
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
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
		mHeaderListAdapter = HeaderListAdapter(layoutInflater, this).apply {
			setHasStableIds(mIsStableHeaderIds)
		}

		mHeaderListView.apply {
			itemAnimator = DefaultItemAnimator().apply {
				supportsChangeAnimations = false
			}
			layoutManager = LinearLayoutManager(this@PreferenceActivityX)
			adapter = mHeaderListAdapter
		}

		val intent = intent
		mShowFragment = intent.getStringExtra(EXTRA_SHOW_FRAGMENT)
		mShowFragmentArguments = intent.getBundleExtra(EXTRA_SHOW_FRAGMENT_ARGUMENTS)

		if (savedInstanceState != null) {
			mIsLargeHeaderIcons = savedInstanceState.getBoolean(KEY_LARGE_HEADER_ICONS)
			title = savedInstanceState.getCharSequence(KEY_TITLE, mDefaultTitle)

			@Suppress("DEPRECATION")
			val targetList = savedInstanceState.getParcelableArrayList<Header>(KEY_HEADERS)
			if (targetList != null) {
				mTargetList.addAll(targetList)

				mHeaderListAdapter.setHeaderList(mTargetList)
			}

			mCurrentFragment = savedInstanceState.getString(KEY_CURRENT_FRAGMENT)
			@Suppress("DEPRECATION")
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

		onBackPressedDispatcher.addCallback(this, mOnBackPressed)
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean {
		when (item.itemId) {
			android.R.id.home -> onBackPressedImpl()
			else -> return super.onOptionsItemSelected(item)
		}
		return true
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
										   preference: Preference): Boolean {
		val parent = fragment.parentFragmentManager
		if (parent.findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
			return true
		}

		if (preference is DialogPreferenceX) {
			val dialogFragment = preference.createDialogFragment()
			if (dialogFragment != null) {
				// PreferenceDialogFragmentCompat still uses getTargetFragment
				// to find the original preference, guess they don't like
				// eating their own dog food :)
				@Suppress("DEPRECATION")
				dialogFragment.setTargetFragment(fragment, 0)
				dialogFragment.show(parent, DIALOG_FRAGMENT_TAG)
				return true
			}
		}

		return false
	}

	fun setUseLargeHeaderIcons(large: Boolean) {
		mIsLargeHeaderIcons = large
	}

	open fun getLargeHeaderIcon(header: Header): Drawable? {
		return ContextCompat.getDrawable(this, header.iconRes)
	}

	class Header() : Parcelable {
		var title: String? = null
		var iconRes: Int = 0
		var fragment: String? = null
		var fragmentArguments: Bundle? = null
		var intent: Intent? = null
		var itemId: Long = -1L

		constructor(parcel: Parcel) : this() {
			title = parcel.readString()
			iconRes = parcel.readInt()
			fragment = parcel.readString()
			fragmentArguments = parcel.readBundle(Bundle::class.java.classLoader)
			@Suppress("DEPRECATION")
			intent = parcel.readParcelable(Intent::class.java.classLoader)
			itemId = parcel.readLong()
		}

		override fun writeToParcel(parcel: Parcel, flags: Int) {
			parcel.writeString(title)
			parcel.writeInt(iconRes)
			parcel.writeString(fragment)
			parcel.writeBundle(fragmentArguments)
			parcel.writeParcelable(intent, flags)
			parcel.writeLong(itemId)
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

	fun invalidateHeader(itemId: Long) {
		for (i in 0 until mTargetList.size) {
			val item = mTargetList.get(i)
			if (item.itemId == itemId) {
				mHeaderListAdapter.notifyItemChanged(i)
				break
			}
		}
	}

	fun invalidateHeaderList(check: (header: Header) -> Boolean) {
		for (i in 0 until mTargetList.size) {
			val item = mTargetList.get(i)
			if (check(item)) {
				mHeaderListAdapter.notifyItemChanged(i)
				break
			}
		}
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

	fun setHideItemId(itemId: Long) {
		mHeaderListView.setHideItemId(itemId)
	}

	fun setHasStableHeaderIds(stable: Boolean) {
		mIsStableHeaderIds = stable
	}

	abstract fun onBuildHeaders(target: MutableList<Header>)

	open fun onCreatedHeaderViewHolder(view: View) {
	}

	open fun onGetNewHeader(): Header? {
		return mTargetList.firstOrNull()
	}

	open fun onBoundHeaderView(view: View, header: Header) {
	}

	open fun validateSwitchToHeader(f: String, args: Bundle?): Header? {
		return null
	}

	private fun onBackPressedImpl() {
		val fm = supportFragmentManager
		if (fm.backStackEntryCount > 0) {
			fm.popBackStackImmediate()
		} else {
			finish()
		}

		if (!isFinishing && fm.backStackEntryCount == 0) {
			if (mShowFragment != null) {
				finish()
			} else {
				if (mIsOnMultiPane) {
					mHeaderListAdapter.setActivatedItem(null)
				} else {
					mHeaderListView.alpha = 0.1f
					mHeaderListView.animate().alpha(1.0f).setDuration(350).start()

					mHeaderListView.visibility = View.VISIBLE
					title = mDefaultTitle
				}

				mCurrentFragment = null
				mCurrentFragmentArguments = null
			}
		}
	}

	private fun rebuildHeaders() {
		val oldActivated = mHeaderListAdapter.getActivatedItem()

		val target = ArrayList<Header>()
		onBuildHeaders(target)

		mTargetList.clear()
		mTargetList.addAll(target)

		mHeaderListAdapter.setHeaderList(mTargetList)

		oldActivated?.fragment?.also {
			val newActivated = findFragmentHeader(it, oldActivated.fragmentArguments)
			mHeaderListAdapter.setActivatedItem(newActivated)
		}

		val showFragment = mShowFragment
		if (showFragment != null) {
			var header = findFragmentHeader(showFragment, mShowFragmentArguments)
			if (header == null) {
				header = validateSwitchToHeader(showFragment, mShowFragmentArguments)
			}
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
			if (mCurrentFragment != fragment ||
					isBundleEquals(mCurrentFragmentArguments, args)) {
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
			}

			if (mIsOnMultiPane) {
				mHeaderListAdapter.setActivatedItem(header)
				for (i in mTargetList.indices) {
					if (mTargetList.get(i) == header) {
						mHeaderListView.smoothScrollToPosition(i)
						break
					}
				}
			} else {
				TransitionManager.beginDelayedTransition(mContentView)
				mHeaderListView.visibility = View.GONE
				title = header.title
			}
		}
	}

	@Suppress("DEPRECATION")
	private fun isBundleEquals(a: Bundle?, b: Bundle?): Boolean {
		if (a == null && b == null) {
			return true
		}
		if (a == null || b == null) {
			return false
		}

		val akeys = a.keySet()
		val bkeys = b.keySet()
		if (akeys != bkeys) {
			return false
		}

		for (ak in akeys) {
			val aval = a.get(ak)
			val bval = b.get(ak)
			if (aval != bval) {
				return false
			}
		}

		return true
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
			if (isBundleEquals(item.fragmentArguments, fragmentArguments)) {
				return item
			}
		}

		return null
	}

	private class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
		val image: ImageView = view.findViewById(R.id.prefs_header_image)

		// val icon: ImageView = view.findViewById(R.id.prefs_header_icon)
		val title: TextView = view.findViewById(R.id.prefs_header_title)
	}

	private class HeaderListAdapter(val inflater: LayoutInflater,
									val activity: PreferenceActivityX)
		: RecyclerView.Adapter<HeaderViewHolder>() {
		val list = ArrayList<Header>()
		var activated: Header? = null

		@SuppressLint("NotifyDataSetChanged")
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

		override fun getItemId(position: Int): Long {
			return list.get(position).itemId
		}

		override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderViewHolder {
			val layoutId =
					if (activity.mIsLargeHeaderIcons) R.layout.prefsx_header_large_icon
					else R.layout.prefsx_header
			val view = inflater.inflate(layoutId, parent, false)
			activity.onCreatedHeaderViewHolder(view)
			return HeaderViewHolder(view).apply {
				view.setOnClickListener(this@HeaderListAdapter::onHeaderClick)
			}
		}

		override fun onBindViewHolder(holder: HeaderViewHolder, position: Int) {
			val item = list.get(position)
			if (item.iconRes != 0) {
				if (activity.mIsLargeHeaderIcons) {
					val largeIcon = activity.getLargeHeaderIcon(item)
					holder.image.setImageDrawable(largeIcon)
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

			activity.onBoundHeaderView(holder.itemView, item)
		}

		private fun onHeaderClick(view: View) {
			val item = view.tag as Header
			activity.onHeaderClick(item)
		}
	}

	class HeaderListView(context: Context, attrs: AttributeSet)
		: RecyclerView(context, attrs) {

		fun setHideItemId(id: Long) {
			if (mHideItemId != id) {
				mHideItemId = id
				invalidate()
			}
		}

		override fun drawChild(canvas: Canvas, child: View, drawingTime: Long): Boolean {
			if (mHideItemId != NO_ID) {
				val holder = getChildViewHolder(child)
				if (holder != null) {
					val adapter = adapter
					val position = holder.bindingAdapterPosition
					if (adapter != null && position != NO_POSITION) {
						if (mHideItemId == adapter.getItemId(position)) {
							return true
						}
					}
				}
			}

			return super.drawChild(canvas, child, drawingTime)
		}

		private var mHideItemId = NO_ID

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

	private val mHandler = Handler(Looper.getMainLooper())
	private lateinit var mRebuildHeaders: () -> Unit

	private var mIsStableHeaderIds = false
	private var mIsLargeHeaderIcons = false

	private val mOnBackPressed = object : OnBackPressedCallback(true) {
		override fun handleOnBackPressed() {
			this@PreferenceActivityX.onBackPressedImpl()
		}
	}

	private lateinit var mContentView: ViewGroup
	private lateinit var mHeaderListView: HeaderListView
	private lateinit var mHeaderListAdapter: HeaderListAdapter
}
