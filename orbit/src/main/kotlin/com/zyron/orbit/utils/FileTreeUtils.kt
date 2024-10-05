package com.zyron.orbit.utils

import com.zyron.orbit.FileTreeNode
import java.io.File

object FileTreeUtils {

    /**
     * Sorts the children of this node, separating directories from files and sorting them alphabetically.
     *
     * @param node The FileTreeNode whose children will be sorted.
     * @return A list of FileTreeNode objects representing the sorted children.
     */
    fun onSortFiles(node: FileTreeNode): List<FileTreeNode> {
        val children = node.file.listFiles()
            ?.asSequence()
            ?.partition { it.isDirectory }
            ?.let { (directories, files) ->
                (directories.sortedBy { it.name.lowercase() } + files.sortedBy { it.name.lowercase() })
            } ?: emptyList()

        return children.map { childFile ->
            FileTreeNode(file = childFile, parent = node, level = node.level + 1)
        }
    }
}