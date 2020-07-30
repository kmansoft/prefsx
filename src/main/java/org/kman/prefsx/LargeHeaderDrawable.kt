package org.kman.prefsx

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat

class LargeHeaderDrawable(context: Context,
						  @DrawableRes iconRes: Int,
						  @ColorRes fillColorRes: Int) : Drawable() {
	private val size = context.resources.getDimensionPixelSize(R.dimen.prefsx_large_header_drawable_size)
	private val icon = ContextCompat.getDrawable(context, iconRes)
	private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
		style = Paint.Style.FILL
		color = ContextCompat.getColor(context, fillColorRes)
	}
	private val rectF = RectF()

	override fun getIntrinsicWidth(): Int {
		return size
	}

	override fun getIntrinsicHeight(): Int {
		return size
	}

	override fun draw(canvas: Canvas) {
		val bounds = bounds
		rectF.set(bounds)
		canvas.drawOval(rectF, fillPaint)

		icon?.also {
			val iw = it.intrinsicWidth
			val ih = it.intrinsicHeight
			val x = bounds.left + (bounds.width() - iw) / 2
			val y = bounds.top + (bounds.height() - ih) / 2
			it.setBounds(x, y, x + iw, y + ih)
			it.draw(canvas)
		}
	}

	override fun setAlpha(alpha: Int) {
		// Nothing
	}

	override fun getOpacity(): Int {
		return PixelFormat.TRANSLUCENT
	}

	override fun setColorFilter(colorFilter: ColorFilter?) {
		// Nothing
	}
}