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

package com.zyron.orbit.adapters

import android.animation.ObjectAnimator
import android.content.Context
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.ChangeBounds
import androidx.transition.TransitionManager
import com.zyron.orbit.R
import com.zyron.orbit.FileTree
import com.zyron.orbit.FileTreeNode
import com.zyron.orbit.FileTreeAdapterUpdateListener
import com.zyron.orbit.databinding.FileTreeViewItemBinding
import com.zyron.orbit.events.FileTreeEventListener
import com.zyron.orbit.providers.DefaultFileIconProvider
import com.zyron.orbit.providers.FileIconProvider
import com.zyron.orbit.widget.FileTreeView
import com.zyron.orbit.utils.FileTreeUtils

/**
 * Adapter for displaying a hierarchical list of files and directories in a RecyclerView.
 *
 * @param context The context for accessing resources and inflating views.
 * @param fileTree The FileTree instance used for managing the file tree data.
 * @param fileTreeIconProvider Provider for file and folder icons.
 * @param fileTreeEventListener Listener for file and folder events (optional).
 */
class FileTreeAdapter(
    private val context: Context,
    private val fileTree: FileTree,
    private val fileIconProvider: FileIconProvider,
    private val fileTreeEventListener: FileTreeEventListener? = null
) : ListAdapter<FileTreeNode, FileTreeAdapter.FileTreeViewHolder>(FileTreeDiffCallback()), FileTreeAdapterUpdateListener {

    @JvmOverloads
    constructor(
        context: Context,
        fileTree: FileTree,
        fileTreeEventListener: FileTreeEventListener? = null
    ) : this(context, fileTree, DefaultFileIconProvider(), fileTreeEventListener)

    private var selectedItemPosition: Int = RecyclerView.NO_POSITION

    inner class FileTreeViewHolder(internal val binding: FileTreeViewItemBinding) : RecyclerView.ViewHolder(binding.root)

    /**
     * Creates a new ViewHolder for a file tree item.
     */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FileTreeViewHolder {
        return FileTreeViewHolder(FileTreeViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    /**
     * Binds data to the ViewHolder for the given position.
     */
    override fun onBindViewHolder(holder: FileTreeViewHolder, position: Int) {
        val node = getItem(position)
        onBindItemView(holder, node, position)
    }
    
    /**
     * Configures the ViewHolder for the file tree item.
     */
    private fun onBindItemView(holder: FileTreeViewHolder, node: FileTreeNode, position: Int) {
        setItemViewLayout(holder, node)
        setItemViewPadding(holder)
        updateItemViewState(holder, node, position)

        when {
            node.file.isDirectory -> onBindDirectory(holder, node)
            node.file.isFile -> onBindFile(holder, node)
        }
    }

    /**
     * Sets the layout parameters for the item view based on the node's level.
     */
    private fun setItemViewLayout(holder: FileTreeViewHolder, node: FileTreeNode) {
        holder.binding.apply {
            val indentationMultiplier = when {
                node.file.isDirectory -> 14 * node.level
                node.file.isFile -> (14 * node.level) + 27.5f 
                else -> 0
            }
            val indentationPx = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, indentationMultiplier.toFloat(), context.resources.displayMetrics).toInt()
            val isLtr = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
            val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
            val layoutParams = root.layoutParams as ViewGroup.MarginLayoutParams

            if (isLtr) {
                layoutParams.leftMargin = indentationPx
                layoutParams.rightMargin = 0
            } else if (isRtl) {
                layoutParams.rightMargin = indentationPx
                layoutParams.leftMargin = 0
            }

            root.layoutParams = layoutParams
        }
    }

    /**
     * Sets padding for the item view and its child views.
     */
    private fun setItemViewPadding(holder: FileTreeViewHolder) {
        holder.binding.apply {
            root.setPadding(4, 8, 4, 8)
            chevronIcon.setPadding(4, 0, 2, 0)            
            fileIcon.setPadding(2, 0, 4, 0)
            fileName.setPadding(6, 7, 7, 6)
        }
    }

    /**
     * Updates the state of the item view based on whether it is selected and expanded.
     */
    private fun updateItemViewState(holder: FileTreeViewHolder, node: FileTreeNode, position: Int) {
        holder.binding.apply {
            root.setBackgroundResource(R.drawable.item_view_background)
            root.isSelected = position == selectedItemPosition && node.isExpanded && node.file.isDirectory
        }
    }

    /**
     * Binds a directory node to the ViewHolder.
     */
    private fun onBindDirectory(holder: FileTreeViewHolder, node: FileTreeNode) {
        holder.binding.apply {
            val isLtr = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_LTR
            val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

            val iconForChevron = when {
                isLtr -> fileIconProvider.getChevronLtrIcon()
                isRtl -> fileIconProvider.getChevronRtlIcon()
                else -> null
            }
            
            val targetRotation = when {
                node.isExpanded && isLtr -> 90f
                node.isExpanded && isRtl -> 270f
                !node.isExpanded && isLtr -> 0f
                !node.isExpanded && isRtl -> 0f
                else -> 0f
            }
            
            chevronIcon.setImageDrawable(iconForChevron?.let { ContextCompat.getDrawable(context, it) })
            chevronIcon.rotation = targetRotation
            chevronIcon.visibility = View.VISIBLE
            fileIcon.setImageDrawable(ContextCompat.getDrawable(context, fileIconProvider.getIconForFolder(node.file)))
            fileName.text = node.file.name

            val currentRotation = chevronIcon.rotation
            if (currentRotation != targetRotation) {
                val rotationAnimator = ObjectAnimator.ofFloat(chevronIcon, "rotation", currentRotation, targetRotation).apply {
                    duration = 300
                    interpolator = LinearInterpolator()
                }
                rotationAnimator.start()
            }

            root.setOnClickListener {
                val previousPosition = selectedItemPosition
                selectedItemPosition = holder.bindingAdapterPosition
                when {
                    !node.isExpanded -> fileTree.setExpandFileTree(node)
                    else -> fileTree.setCollapseFileTree(node) 
                }
                notifyItemChanged(selectedItemPosition)
                notifyItemChanged(previousPosition)
            }

            root.setOnLongClickListener {
                fileTreeEventListener?.onFolderLongClick(node.file) ?: false
            }
        }
    }

    /**
     * Binds a file node to the ViewHolder.
     */
    private fun onBindFile(holder: FileTreeViewHolder, node: FileTreeNode) {
        holder.binding.apply {
            chevronIcon.visibility = View.GONE
            fileIcon.setImageDrawable(ContextCompat.getDrawable(context, fileIconProvider.getIconForFile(node.file)))
            fileName.text = node.file.name

            root.setOnClickListener {
                fileTreeEventListener?.onFileClick(node.file)
            }

            root.setOnLongClickListener {
                fileTreeEventListener?.onFileLongClick(node.file) ?: false
            }
        }
    }

    /**
     * Updates the RecyclerView when the file tree changes.
     */
    override fun onFileTreeUpdated(startPosition: Int, itemCount: Int) {
        notifyItemRangeChanged(startPosition, itemCount)
    }

    /**
     * Submits a new list to be displayed.
     */
    override fun submitList(list: MutableList<FileTreeNode>?) {
        super.submitList(list?.toList())
    }
    
    /**
     * A DiffUtil.ItemCallback implementation for comparing FileTreeNode objects.
     */    
    class FileTreeDiffCallback : DiffUtil.ItemCallback<FileTreeNode>() {
    
       /**
        * Determines if the contents of two FileTreeNode items are the same.
        *
        * @param oldItem The previous FileTreeNode item.
        * @param newItem The new FileTreeNode item.
        * @return True if the contents of the two items are the same, false otherwise.
        */    
        override fun areItemsTheSame(oldItem: FileTreeNode, newItem: FileTreeNode): Boolean {
            return oldItem.file.name == newItem.file.name
        }

       /**
        * Determines if two FileTreeNode items represent the same file by comparing their absolute paths.
        *
        * @param oldItem The previous FileTreeNode item.
        * @param newItem The new FileTreeNode item.
        * @return True if the two items represent the same file, false otherwise.
        */
        override fun areContentsTheSame(oldItem: FileTreeNode, newItem: FileTreeNode): Boolean {
            return oldItem.file.path == newItem.file.path
        }
    }    
}