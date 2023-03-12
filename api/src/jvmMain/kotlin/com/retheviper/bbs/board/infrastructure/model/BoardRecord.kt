package com.retheviper.bbs.board.infrastructure.model

import com.retheviper.bbs.common.value.BoardId

data class BoardRecord(
    val id: BoardId,
    val name: String,
    val description: String?
)