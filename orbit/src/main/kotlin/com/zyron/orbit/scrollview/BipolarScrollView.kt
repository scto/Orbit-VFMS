package com.zyron.orbit.scrollview

import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.OverScroller
import androidx.core.view.GestureDetectorCompat
import androidx.core.view.ViewCompat

class BipolarScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    private var scroller: OverScroller = OverScroller(context)
    private var gestureDetector: GestureDetectorCompat
    private var lastMotionX: Float = 0f
    private var lastMotionY: Float = 0f
    private var isBeingDragged = false
    private var activePointerId = -1

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        gestureDetector = GestureDetectorCompat(context, object : GestureDetector.SimpleOnGestureListener() {
        /*    override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent?,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                scrollBy(distanceX.toInt(), distanceY.toInt())
                return true
            }

            override fun onFling(
                e1: MotionEvent?, 
                e2: MotionEvent?, 
                velocityX: Float, 
                velocityY: Float
            ): Boolean {
                fling(-velocityX.toInt(), -velocityY.toInt())
                return true
            }*/
        })
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var maxWidth = 0
        var maxHeight = 0
        var childState = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, 0)
                val lp = child.layoutParams as MarginLayoutParams
                maxWidth = maxWidth.coerceAtLeast(child.measuredWidth + lp.leftMargin + lp.rightMargin)
                maxHeight = maxHeight.coerceAtLeast(child.measuredHeight + lp.topMargin + lp.bottomMargin)
                childState = combineMeasuredStates(childState, child.measuredState)
            }
        }

        maxWidth += paddingLeft + paddingRight
        maxHeight += paddingTop + paddingBottom

        val width = resolveSizeAndState(maxWidth, widthMeasureSpec, childState)
        val height = resolveSizeAndState(maxHeight, heightMeasureSpec, childState shl MEASURED_HEIGHT_STATE_SHIFT)
        setMeasuredDimension(width, height)
    }

    override fun onLayout(p0: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        var childLeft = paddingLeft
        var childTop = paddingTop

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as MarginLayoutParams
                val childWidth = child.measuredWidth
                val childHeight = child.measuredHeight

                val cl = childLeft + lp.leftMargin
                val ct = childTop + lp.topMargin
                val cr = cl + childWidth
                val cb = ct + childHeight

                child.layout(cl, ct, cr, cb)

                childLeft += childWidth + lp.leftMargin + lp.rightMargin
                childTop += childHeight + lp.topMargin + lp.bottomMargin
            }
        }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(ev) || super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        return gestureDetector.onTouchEvent(ev) || super.onTouchEvent(ev)
    }

    private fun fling(velocityX: Int, velocityY: Int) {
        scroller.fling(
            scrollX, scrollY, velocityX, velocityY,
            0, getScrollRangeX(), 0, getScrollRangeY()
        )
        ViewCompat.postInvalidateOnAnimation(this)
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollTo(scroller.currX, scroller.currY)
            postInvalidateOnAnimation()
        }
    }

    private fun getScrollRangeX(): Int {
        val child = getChildAt(0)
        return (child.measuredWidth - width).coerceAtLeast(0)
    }

    private fun getScrollRangeY(): Int {
        val child = getChildAt(0)
        return (child.measuredHeight - height).coerceAtLeast(0)
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    override fun generateDefaultLayoutParams(): LayoutParams {
        return MarginLayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
    }

    override fun checkLayoutParams(p: LayoutParams): Boolean {
        return p is MarginLayoutParams
    }
}