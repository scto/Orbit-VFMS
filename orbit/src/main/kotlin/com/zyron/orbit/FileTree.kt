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

package com.zyron.orbit

import android.content.Context
import android.util.Log
import com.zyron.orbit.FileTreeNode
import com.zyron.orbit.map.FileMapManager
import com.zyron.orbit.map.ConcurrentFileMap
import com.zyron.orbit.utils.FileTreeUtils
import kotlinx.coroutines.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Listener interface to notify when the file tree is updated.
 */
interface FileTreeAdapterUpdateListener {
    fun onFileTreeUpdated(startPosition: Int, itemCount: Int)
}

/**
 * Represents a node in a file tree, corresponding to a file or directory.
 *
 * @property file The file or directory represented by this node.
 * @property parent The parent node of this node in the file tree.
 * @property level The depth level of this node in the file tree.
 */
data class FileTreeNode(var file: File, var parent: FileTreeNode? = null, var level: Int = 0) {
    var isExpanded: Boolean = false
    var childrenStartIndex: Int = 0
    var childrenEndIndex: Int = 0
    var childrenLoaded: Boolean = false
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        other as FileTreeNode
        return this.file.absolutePath == other.file.absolutePath
    }

    override fun hashCode(): Int {
        return file.absolutePath.hashCode()
    }    
}

/**
 * FileTree represents a file tree structure with functionality to expand and collapse nodes.
 *
 * @property context The application context.
 * @property rootDirectory The root directory path of the file tree.
 */
class FileTree(private val context: Context, private val rootDirectory: String) {

    private val nodes: MutableList<FileTreeNode> = mutableListOf()
    private val expandedNodes: MutableSet<FileTreeNode> = mutableSetOf()
    private var adapterUpdateListener: FileTreeAdapterUpdateListener? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isSingleExpansionEnabled: Boolean = false  
    private var isSingleCollapsingEnabled: Boolean = false  
    private var isMainRecursiveExpansionEnabled: Boolean = false  
    private var isMainRecursiveCollapsingEnabled: Boolean = false  
    private var isRecursiveExpansionEnabled: Boolean = false  
    private var isRecursiveCollapsingEnabled: Boolean = true  
        
    /**
     * Sets the listener for adapter updates.
     *
     * @param listener The listener to be notified of updates.
     */
    fun setAdapterUpdateListener(listener: FileTreeAdapterUpdateListener) {
        this.adapterUpdateListener = listener
    }
    
    fun getSingleExpansionEnabled(state: Boolean) {
        this.isSingleExpansionEnabled = state
    }
    
    fun getSingleCollapsingEnabled(state: Boolean) {
        this.isSingleCollapsingEnabled = state
    }   
    
    fun getMainRecursiveExpansionEnabled(state: Boolean) {
        this.isMainRecursiveExpansionEnabled = state
    }
    
    fun getMainRecursiveCollapsingEnabled(state: Boolean) {
        this.isMainRecursiveCollapsingEnabled = state
    }             
    
    fun getRecursiveExpansionEnabled(state: Boolean) {
        this.isRecursiveExpansionEnabled = state
    }
    
    fun getRecursiveCollapsingEnabled(state: Boolean) {
        this.isRecursiveCollapsingEnabled = state
    }    

    init {
        val file = File(rootDirectory)
        val rw = file.canRead() && file.canWrite()
        if (!file.exists() || !rw) {
            Log.e(this::class.java.simpleName, "The provided path: $rootDirectory is invalid or does not exist.")
            Log.d(this::class.java.simpleName, "Continuing anyway...")
        }

        coroutineScope.coroutineContext[Job]?.invokeOnCompletion { exception ->
            if (exception is CancellationException) {
                Log.d(this::class.java.simpleName, "FileTree operation was cancelled.")
            }
        }

        coroutineScope.launch {
            try {
                val rootPath = Paths.get(rootDirectory)
                val rootNode = FileTreeNode(rootPath.toFile())
                addNode(rootNode)
                FileMapManager.startFileMapping(nodes)
                onSingleExpansion(rootNode)
            } catch (e: Exception) {
                Log.e(this::class.java.simpleName, "Error initializing FileTree: ${e.localizedMessage}", e)
            }
        }
    }   
    
    fun setExpandFileTree(file: FileTreeNode) {
        when {
            isSingleExpansionEnabled -> onSingleExpansion(file)
            isMainRecursiveExpansionEnabled -> onMainRecursiveExpansion(file)
            isRecursiveExpansionEnabled -> onRecursiveExpansion(file)
            else -> onSingleExpansion(file)
        }
    }
    
    fun setCollapseFileTree(file: FileTreeNode) {
          when {
            isSingleExpansionEnabled -> onSingleCollapsing(file)
            isMainRecursiveExpansionEnabled -> onMainRecursiveCollapsing(file)
            isRecursiveExpansionEnabled -> onRecursiveCollapsing(file)
            else -> onRecursiveCollapsing(file)
        }    
    }

    /**
     * Returns the list of all nodes in the file tree.
     *
     * @return List of FileTreeNode objects.
     */
    fun getNodes(): List<FileTreeNode> = nodes

    /**
     * Returns the set of expanded nodes in the file tree.
     *
     * @return Set of expanded FileTreeNode objects.
     */
    fun getExpandedNodes(): Set<FileTreeNode> = expandedNodes

    /**
     * Adds a node to the file tree and notifies the adapter.
     *
     * @param node The node to add.
     * @param parent The parent node, if any.
     */
    private suspend fun addNode(node: FileTreeNode, parent: FileTreeNode? = null) {
        node.parent = parent
        nodes.add(node)
        withContext(Dispatchers.Main) {
            adapterUpdateListener?.onFileTreeUpdated(nodes.indexOf(node), 1)
        }
    }

    /**
     * Expands a node to display its children.
     *
     * @param node The node to expand.
     */
    fun onSingleExpansion(node: FileTreeNode) {
        if (!node.isExpanded && Files.isDirectory(node.file.toPath())) {
            node.isExpanded = true
            coroutineScope.launch {
                try {
                    expandedNodes.add(node)

                    val newNodes: List<FileTreeNode>? = synchronized(ConcurrentFileMap.concurrentFileMap) {
                        ConcurrentFileMap.concurrentFileMap[node] ?: FileTreeUtils.onSortFiles(node)
                    }

                    newNodes?.let {
                        if (it.isNotEmpty()) {
                            val insertIndex = nodes.indexOf(node) + 1
                            node.childrenStartIndex = insertIndex
                            node.childrenEndIndex = insertIndex + it.size
                            nodes.addAll(insertIndex, it)
                            withContext(Dispatchers.Main) {
                                adapterUpdateListener?.onFileTreeUpdated(node.childrenStartIndex, it.size)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(this::class.java.simpleName, "Error expanding node: ${e.localizedMessage}", e)
                }
            }
        }
    }
    
    fun onSingleCollapsing(node: FileTreeNode) {
    }
    
    fun onMainRecursiveExpansion(node: FileTreeNode) {
    }
    
    fun onMainRecursiveCollapsing(node: FileTreeNode) {
    }
    
    fun onRecursiveExpansion(node: FileTreeNode) {
    }

    /**
     * Collects all children of a node recursively.
     *
     * @param node The parent node.
     * @param nodesToRemove The list to which all children will be added.
     */
    private fun collectAllChildren(node: FileTreeNode, nodesToRemove: MutableList<FileTreeNode>) {
        val children = nodes.filter { it.parent == node }
        nodesToRemove.addAll(children)
        children.forEach { childNode ->
            collectAllChildren(childNode, nodesToRemove)
        }
    }

    /**
     * Collapses a node, hiding its children.
     *
     * @param node The node to collapse.
     */
    fun onRecursiveCollapsing(node: FileTreeNode) {
        if (node.isExpanded) {
            node.isExpanded = false
            coroutineScope.launch {
                try {
                    expandedNodes.remove(node)
                    val nodesToRemove = mutableListOf<FileTreeNode>()
                    collectAllChildren(node, nodesToRemove)
                    nodesToRemove.forEach { childNode ->
                        childNode.isExpanded = false
                        expandedNodes.remove(childNode)
                    }
                    nodes.removeAll(nodesToRemove)
                    withContext(Dispatchers.Main) {
                        adapterUpdateListener?.onFileTreeUpdated(nodes.indexOf(node), nodesToRemove.size)
                    }
                } catch (e: Exception) {
                    Log.e(this::class.java.simpleName, "Error collapsing node: ${e.localizedMessage}", e)
                }
            }
        }
    }
    
    /**
     * Expands all child nodes of a specified node recursively.
     *
     * @param node The parent node whose children should be expanded.
     */
    fun onExpandDescendents (node: FileTreeNode) {
        if (!node.isExpanded && Files.isDirectory(node.file.toPath())) {
            onSingleExpansion(node) // Expand the current node first
        }

        // Now, expand all child nodes recursively
        val children = nodes.filter { it.parent == node }
        children.forEach { childNode ->
            onExpandDescendents(childNode) // Recursively expand each child node
        }
    }

    /**
     * Collapses all child nodes of a specified node recursively.
     *
     * @param node The parent node whose children should be collapsed.
     */
    fun onCollapseDescendents (node: FileTreeNode) {
        if (node.isExpanded) {
            onRecursiveCollapsing(node) // Collapse the current node first
        }

        // Now, collapse all child nodes recursively
        val children = nodes.filter { it.parent == node }
        children.forEach { childNode ->
            onCollapseDescendents(childNode) // Recursively collapse each child node
        }
    }

    /**
     * Cancels all coroutines related to this FileTree.
     */
    fun cancelAllCoroutines() {
        coroutineScope.cancel()
    }

    /**
     * Cleans up resources when the FileTree is destroyed.
     */
    fun onDestroy() {
        coroutineScope.coroutineContext[Job]?.cancelChildren()
    }
}