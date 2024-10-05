package com.zyron.orbit.widget

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.zyron.orbit.adapters.FilePathAdapter
import com.zyron.orbit.viewmodel.FileListViewModel
import java.io.File

class FilePathView : RecyclerView {

  constructor(context: Context) : super(context)

  constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)

  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int,) : super(context, attrs, defStyleAttr)

  private val adapter = FilePathAdapter()

  init {
    layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
    setAdapter(adapter)
  }

  fun setFileExplorerViewModel(viewModel: FileListViewModel) {
    adapter.setFileExplorerViewModel(viewModel)
  }

  fun setPath(path: String) {
    if (path.startsWith("/data")) {
      adapter.setPath(null)
      return
    }

    adapter.setPath(File(path))

    scrollToPosition(adapter.itemCount - 1)
  }
}