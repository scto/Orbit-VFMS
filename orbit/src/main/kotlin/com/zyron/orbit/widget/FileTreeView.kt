/**
 * Copyright 2024 Zyron Official.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zyron.orbit.widget

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet
import android.view.View
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.animation.AnimationUtils
import android.widget.EdgeEffect
import android.widget.OverScroller
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import androidx.recyclerview.widget.SimpleItemAnimator
import com.zyron.orbit.R
import com.zyron.orbit.FileTree
import com.zyron.orbit.FileTreeAdapterUpdateListener
import com.zyron.orbit.adapters.FileTreeAdapter
import com.zyron.orbit.events.FileTreeEventListener
import com.zyron.orbit.providers.FileIconProvider
import com.zyron.orbit.map.ConcurrentFileMap
import com.zyron.orbit.utils.ThreadUtils

/**
 * FileTreeView extends RecyclerView to represent a file tree structure.
 * Handles initialization, layout, and interactions specific to a file tree.
 */
class FileTreeView : RecyclerView {
    
    private var path: String? = null
    private var fileTree: FileTree? = null

    private var recyclerItemViewCount: Int = 200
    private var recyclerItemViewEnabled: Boolean = true
    private var itemViewCacheSize: Int = 100
    private var itemViewCachingEnabled: Boolean = true
    private var fileMapMaxSize: Int = 150
    private var fileMapEnabled: Boolean = true
    private var fileTreeAnimation: Int = 4
    private var fileTreeAnimationEnabled: Boolean = true
    private var animationResId: Int = 4
    private var animation: Int = 0
    
    private final var FALLDOWN_ANIM = 0
    private final var ROTATE_IN_ANIM = 1
    private final var SCALE_UP_ANIM = 2
    private final var SCALE_IN_ANIM = 3
    private final var DEFAULT_ANIM = 4

    private lateinit var recycledViewPool: RecycledViewPool
    
    private var startX = 0f
    private var startY = 0f
    private var isHorizontalScroll = false

    constructor(context: Context) : super(context) {
        initialize(context, null)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        initialize(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defstyleAttrs: Int) : super(context, attrs, defstyleAttrs) {
        initialize(context, attrs)
    }

    /**
     * Initializes the FileTreeView with given attributes.
     *
     * @param context The context associated with the view.
     * @param attrs The attributes from the XML layout.
     */
    private fun initialize(context: Context, attrs: AttributeSet?) {
        attrs?.let {
            val attribute = context.obtainStyledAttributes(it, R.styleable.FileTreeView)
            recyclerItemViewCount = attribute.getInteger(R.styleable.FileTreeView_recyclerItemViewCount, 200)
            recyclerItemViewEnabled = attribute.getBoolean(R.styleable.FileTreeView_recyclerItemViewEnabled, true)
            itemViewCacheSize = attribute.getInteger(R.styleable.FileTreeView_itemViewCacheSize, 100)
            itemViewCachingEnabled = attribute.getBoolean(R.styleable.FileTreeView_itemViewCachingEnabled, true)
            fileMapMaxSize = attribute.getInteger(R.styleable.FileTreeView_fileMapMaxSize, 150)
            fileMapEnabled = attribute.getBoolean(R.styleable.FileTreeView_fileMapEnabled, true)
            fileTreeAnimation = attribute.getInteger(R.styleable.FileTreeView_fileTreeAnimation, 4)
            fileTreeAnimationEnabled = attribute.getBoolean(R.styleable.FileTreeView_fileTreeAnimationEnabled, true)
            attribute.recycle()
        }
        if (itemViewCachingEnabled) {
            setItemViewCacheSize(itemViewCacheSize)
        }
        setAnimation(fileTreeAnimation)
    }

    /**
     * Sets the animation for the FileTreeView based on the provided animation type.
     *
     * @param fileTreeAnimation The type of animation to use.
     */
    private fun setAnimation(fileTreeAnimation: Int) {
        animationResId = when (fileTreeAnimation) {
            0 -> R.anim.fall_down_animation
            1 -> R.anim.rotate_in_animation
            2 -> R.anim.scale_up_animation
            3 -> R.anim.slide_in_animation
            else -> R.anim.default_animation
        }
    }
    
    private fun setRecursiveExpansionEnabled(state: Boolean) = fileTree?.getRecursiveExpansionEnabled(state) ?: false

    private fun setRecursiveCollapsingEnabled(state: Boolean) = fileTree?.getRecursiveCollapsingEnabled(state) ?: true
    
    /**
     * Initializes the FileTree with a given path and optional listeners.
     *
     * @param path The root path for the file tree.
     * @param fileTreeEventListener Optional listener for file tree events.
     * @param fileTreeIconProvider Optional provider for custom file tree icons.
     */
    fun initializeFileTree(path: String, fileTreeEventListener: FileTreeEventListener? = null, fileIconProvider: FileIconProvider? = null) {
        this.path = path
        fileTree = FileTree(context, path)

        val fileTreeAdapter = when {
            fileTreeEventListener == null -> FileTreeAdapter(context, fileTree!!)
            fileIconProvider != null -> FileTreeAdapter(context, fileTree!!, fileIconProvider, fileTreeEventListener)
            else -> FileTreeAdapter(context, fileTree!!, fileTreeEventListener)
        }

        recycledViewPool = RecycledViewPool()
        this.setRecycledViewPool(recycledViewPool)

        if (recyclerItemViewEnabled) {
            recycledViewPool.setMaxRecycledViews(0, recyclerItemViewCount)
        }

        if (fileMapEnabled) {
            val concurrentFileMap = ConcurrentFileMap(fileTree!!.getNodes(), fileMapMaxSize)
            Thread(concurrentFileMap).start()
        }

        if (fileTreeAnimationEnabled) {
            val animation = AnimationUtils.loadLayoutAnimation(context, animationResId)
            this.layoutAnimation = animation
        }

        layoutManager = LinearLayoutManager(context)
        adapter = fileTreeAdapter
        this.requestLayout()

        (this.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = true     

        fileTree!!.setAdapterUpdateListener(object : FileTreeAdapterUpdateListener {
            override fun onFileTreeUpdated(startPosition: Int, itemCount: Int) {
                fileTreeEventListener?.onFileTreeViewUpdated(startPosition, itemCount)
                ThreadUtils.runOnUiThread {
                    fileTreeAdapter.submitList(fileTree!!.getNodes().toMutableList())
                }
            }
        })
    }
    
    override fun onInterceptTouchEvent(e: MotionEvent): Boolean {
        when (e.action) {
            MotionEvent.ACTION_DOWN -> {
                // Record the initial touch positions
                startX = e.x
                startY = e.y
                isHorizontalScroll = false  // Reset horizontal scroll flag
            }
            MotionEvent.ACTION_MOVE -> {
                // Calculate the delta for horizontal and vertical movement
                val dx = kotlin.math.abs(e.x - startX)
                val dy = kotlin.math.abs(e.y - startY)

                // Determine if it's horizontal or vertical scrolling
                if (dx > dy) {
                    isHorizontalScroll = true
                    return false  // Allow horizontal scroll to proceed
                }
            }
        }
        return super.onInterceptTouchEvent(e)  // Default handling for vertical scrolling
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        if (isHorizontalScroll) {
            // Handle horizontal scroll
            scrollBy(-e.x.toInt(), 0)  // Scroll horizontally
            return true
        }
        return super.onTouchEvent(e)  // Default handling for vertical scrolling
    } 
}