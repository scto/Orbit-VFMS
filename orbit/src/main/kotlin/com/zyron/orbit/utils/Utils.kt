package com.zyron.orbit.utils

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.appcompat.widget.TintTypedArray

object Utils {
/**
     * Retrieves the color value associated with a given attribute resource from the current theme.
     *
     * @param attr The attribute resource ID for which to retrieve the color.
     * @return The color value associated with the attribute, as defined in the current theme.
     */
    @SuppressLint("RestrictedApi")
    @ColorInt
    fun Context.getAttrColor(@AttrRes attr: Int): Int {
        return TintTypedArray.obtainStyledAttributes(this, null, intArrayOf(attr), 0, 0)
            .getColorStateList(0)
            .defaultColor
    }
}