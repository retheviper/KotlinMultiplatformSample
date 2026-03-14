package com.retheviper.chat.app

expect object LinkAssetActions {
    fun saveRemoteFile(url: String, suggestedName: String? = null): Boolean
    fun copyText(text: String): Boolean
}
