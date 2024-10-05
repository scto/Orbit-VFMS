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
import com.zyron.orbit.utils.FileTreeUtils
import com.zyron.orbit.utils.PathUtils
import com.zyron.orbit.utils.FileUtils
import com.zyron.orbit.FileTreeNode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class FileTreeViewModel : ViewModel() {

    // LiveData to store the list of FileTreeNode objects in the current directory
    private val _fileTreeNodes = MutableLiveData<List<FileTreeNode>>(emptyList())
    val fileTreeNodes: LiveData<List<FileTreeNode>> = _fileTreeNodes

    // LiveData to store the current path
    private val _currentPath = MutableLiveData<String>(PathUtils.getRootPathExternalFirst())
    val currentPath: LiveData<String> = _currentPath

    // FileTreeNode representing the current directory node
    private var currentFileTreeNode: FileTreeNode = FileTreeNode(File(_currentPath.value!!), null, 0)

    /**
     * Navigates to the parent directory.
     */
    fun backPath() {
        if (_currentPath.value == PathUtils.getRootPathExternalFirst()) {
            return
        }
        setCurrentPath(FileUtils.getParentDirPath(_currentPath.value!!))
    }

    /**
     * Sets a new current path and refreshes the list of FileTreeNodes.
     */
    fun setCurrentPath(path: String) {
        _currentPath.value = path
        currentFileTreeNode = FileTreeNode(File(path), null, 0)
        refreshFileTreeNodes()
    }

    /**
     * Refreshes the list of sorted FileTreeNodes for the current directory.
     */
    fun refreshFileTreeNodes() {
        viewModelScope.launch(Dispatchers.IO) {
            val sortedNodes = FileTreeUtils.onSortFiles(currentFileTreeNode)
            withContext(Dispatchers.Main) {
                _fileTreeNodes.value = sortedNodes
            }
        }
    }

    /**
     * Renames a file and refreshes the list of FileTreeNodes.
     */
    fun renameFile(oldFile: File, newFileName: String) {
        val newFile = File(oldFile.parentFile, newFileName)
        if (oldFile.renameTo(newFile)) {
            refreshFileTreeNodes() // Refresh after renaming
        }
    }

    /**
     * Deletes a file and refreshes the list of FileTreeNodes.
     */
    fun deleteFile(file: File) {
        if (file.delete()) {
            refreshFileTreeNodes() // Refresh after deleting
        }
    }

    /**
     * Creates a new file in the current directory and refreshes the list of FileTreeNodes.
     */
    fun createFile(dir: File, fileName: String) {
        val newFile = File(dir, fileName)
        if (newFile.createNewFile()) {
            refreshFileTreeNodes() // Refresh after creating
        }
    }
}