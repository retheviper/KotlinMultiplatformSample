package com.retheviper.chat.app

actual object LinkAssetActions {
    actual fun saveRemoteFile(url: String, suggestedName: String?): Boolean = false
}
