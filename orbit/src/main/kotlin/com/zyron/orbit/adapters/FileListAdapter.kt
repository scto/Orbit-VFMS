package com.zyron.orbit.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.zyron.orbit.databinding.FileListViewItemBinding
import com.zyron.orbit.events.FileListEventListener
import com.zyron.orbit.providers.DefaultFileIconProvider
import com.zyron.orbit.providers.FileIconProvider
import com.zyron.orbit.R
import java.io.File

class FileListAdapter(
    private val context: Context, 
    private val fileIconProvider: FileIconProvider, 
    private val fileListEventListener: FileListEventListener? = null
) : ListAdapter<File, FileListAdapter.VH>(FileDiffCallback()) {

    @JvmOverloads
    constructor(
        context: Context, 
        fileListEventListener: FileListEventListener? = null
    ) : this(context, DefaultFileIconProvider(), fileListEventListener)

    inner class VH(internal val binding: FileListViewItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        return VH(FileListViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val file = getItem(position)

        holder.binding.apply {
            name.text = file.name
            info.setImageDrawable(ContextCompat.getDrawable(context, fileIconProvider.getOptionIcon()))
            root.setBackgroundResource(R.drawable.item_view_background)
            onBindItemView(holder, file, position)  
        }
    }

    /**
     * Configures the ViewHolder for the file tree item.
     */
    private fun onBindItemView(holder: VH, file: File, position: Int) {
        setItemViewPadding(holder)
        when {
            file.isDirectory -> onBindDirectory(holder, file) 
            file.isFile -> onBindFile(holder, file)  
        }
    }

    /**
     * Sets padding for the item view and its child views.
     */
    private fun setItemViewPadding(holder: VH) {
        holder.binding.apply {
            root.setPadding(4, 8, 4, 8)
            icon.setPadding(2, 0, 4, 0)
            name.setPadding(6, 7, 7, 6)
            info.setPadding(4, 0, 2, 0)
        }
    }

    /**
     * Binds a directory node to the ViewHolder.
     */
    private fun onBindDirectory(holder: VH, file: File) {
        holder.binding.apply {
            icon.setImageDrawable(
                ContextCompat.getDrawable(context, fileIconProvider.getIconForFolder(file))
            )
            name.text = file.name

            root.setOnClickListener {
                fileListEventListener?.onFolderClick(file)  
            }

            root.setOnLongClickListener {
                fileListEventListener?.onFolderLongClick(file) ?: false  
            }
        }
    }

    /**
     * Binds a file node to the ViewHolder.
     */
    private fun onBindFile(holder: VH, file: File) {
        holder.binding.apply {
            icon.setImageDrawable(
                ContextCompat.getDrawable(context, fileIconProvider.getIconForFile(file))
            )
            name.text = file.name

            root.setOnClickListener {
                fileListEventListener?.onFileClick(file)
            }

            root.setOnLongClickListener {
                fileListEventListener?.onFileLongClick(file) ?: false
            }
        }
    }
    
    /**
     * Submits a new list to be displayed.
     */
    override fun submitList(list: MutableList<File>?) {
        super.submitList(list?.toList())
    }

    class FileDiffCallback : DiffUtil.ItemCallback<File>() {
    
        override fun areItemsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: File, newItem: File): Boolean {
            return oldItem.path == newItem.path
        }
    }
}