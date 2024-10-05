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
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zyron.orbit.adapters.FileListAdapter
import com.zyron.orbit.events.FileListEventListener
import com.zyron.orbit.providers.FileIconProvider
import com.zyron.orbit.viewmodel.FileListViewModel
import java.io.File

class FileListView : RecyclerView {

  private var path: String? = null

  constructor(context: Context) : super(context) {
      initialize(context, null)
  }

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
      initialize(context, attrs)
  }

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int,) : super(context, attrs, defStyleAttr) {
      initialize(context, attrs)
  }

  private fun initialize(context: Context, attrs: AttributeSet?) {
  }
  
    /**
     * Initializes the FileList with a given path and optional listeners.
     *
     * @param path The root path for the file tree.
     * @param fileListEventListener Optional listener for file tree events.
     * @param fileIconProvider Optional provider for custom file tree icons.
     */
    fun initializeFileList(path: String, fileListEventListener: FileListEventListener? = null, fileIconProvider: FileIconProvider? = null) {
        this.path = path

        val fileListAdapter = when {
            fileListEventListener == null -> FileListAdapter(context, null)
            fileIconProvider != null -> FileListAdapter(context, fileIconProvider, fileListEventListener)
            else -> FileListAdapter(context, fileListEventListener)
        }

        layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        adapter = fileListAdapter
        setAdapter(adapter)
        this.requestLayout()
    }  

/*  fun setFileListViewModel(viewModel: FileListViewModel) {
    fileListAdapter.setFileListViewModel(viewModel)
  }

  fun setPath(path: String) {
    if (path.startsWith("/data")) {
      fileListAdapter.setPath(null)
      return
    }

    fileListAdapter.setPath(File(path))

    scrollToPosition(fileListAdapter.itemCount - 1)
  }*/
}