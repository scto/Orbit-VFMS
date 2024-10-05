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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.R
import com.zyron.orbit.databinding.FilePathViewItemBinding
import com.zyron.orbit.utils.Utils.getAttrColor
import com.zyron.orbit.viewmodel.FileListViewModel
import java.io.File

class FilePathAdapter : RecyclerView.Adapter<FilePathAdapter.VH>() {

  private val paths: MutableList<File> = ArrayList()

  private var viewModel: FileListViewModel? = null

  inner class VH(internal val binding: FilePathViewItemBinding) :
    RecyclerView.ViewHolder(binding.root)

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
    return VH(FilePathViewItemBinding.inflate(LayoutInflater.from(parent.context), parent, false))
  }

  override fun onBindViewHolder(holder: VH, position: Int) {
    holder.binding.apply {
      val file = paths[position]

      val colorPrimary = root.context.getAttrColor(R.attr.colorPrimary)
      val colorControlNormal = root.context.getAttrColor(R.attr.colorControlNormal)

      name.text = when (file.name) {
        "0" -> "Storage"
        "sdcard" ->"SdCard"
        else -> file.name
      }

      if (position == itemCount - 1) {
        name.setTextColor(colorPrimary)
        separator.visibility = View.GONE
      } else {
        name.setTextColor(colorControlNormal)
        separator.visibility = View.VISIBLE
      }
      name.setOnClickListener { viewModel?.setCurrentPath(file.absolutePath) }
    }
  }

  override fun getItemCount(): Int {
    return paths.size
  }

  fun setFileExplorerViewModel(viewModel: FileListViewModel) {
    this.viewModel = viewModel
  }

  @SuppressLint("NotifyDataSetChanged")
  fun setPath(path: File?) {
    paths.clear()

    var temp: File? = path
    while (temp != null) {
      if (temp.absolutePath.equals("/storage/emulated")) {
        break
      }
      paths.add(temp)
      temp = temp.parentFile
    }

    paths.reverse()
    notifyDataSetChanged()
  }
}