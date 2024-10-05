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
 
package com.zyron.orbit.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zyron.orbit.utils.PathUtils
import com.zyron.orbit.utils.FileUtils
import java.io.File
import java.util.Arrays
import java.util.Comparator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FileListViewModel : ViewModel() {

companion object {
  val FOLDER_FIRST_ORDER: Comparator<File> = compareBy<File> { file -> if (file.isFile) 1 else 0 }.thenBy { it.name.lowercase() }
}

  private val _files = MutableLiveData<List<File>>(emptyList())
  private val _currentPath = MutableLiveData(PathUtils.getRootPathExternalFirst())

  val files: LiveData<List<File>> = _files
  val currentPath: LiveData<String> = _currentPath

  fun backPath() {
    if (_currentPath.value.equals(PathUtils.getRootPathExternalFirst())) {
      return
    }
    setCurrentPath(FileUtils.getParentDirPath(_currentPath.value!!))
  }

  fun setCurrentPath(path: String) {
    _currentPath.value = path
    refreshFiles()
  }

  fun refreshFiles() {
    viewModelScope.launch(Dispatchers.IO) {
      val listFiles = _currentPath.value?.let { File(it).listFiles() }

      val files = mutableListOf<File>()

      if (listFiles != null) {
        Arrays.sort(listFiles, FOLDER_FIRST_ORDER)
        for (file in listFiles) {
          if (file.isHidden) {
            continue
          }
          files.add(file)
        }
      }
      withContext(Dispatchers.Main) { _files.value = files }
    }
  }
}