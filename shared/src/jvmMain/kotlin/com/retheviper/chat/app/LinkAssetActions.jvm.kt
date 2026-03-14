package com.retheviper.chat.app

import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.BufferedInputStream
import java.net.URL
import java.nio.file.Files
import javax.swing.JFileChooser

actual object LinkAssetActions {
    actual fun saveRemoteFile(url: String, suggestedName: String?): Boolean {
        val chooser = JFileChooser().apply {
            selectedFile = java.io.File(suggestedName ?: url.substringAfterLast('/').ifBlank { "download" })
        }
        if (chooser.showSaveDialog(null) != JFileChooser.APPROVE_OPTION) {
            return false
        }

        val target = chooser.selectedFile.toPath()
        BufferedInputStream(URL(url).openStream()).use { input ->
            Files.copy(input, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
        }
        return true
    }

    actual fun copyText(text: String): Boolean {
        Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
        return true
    }
}
