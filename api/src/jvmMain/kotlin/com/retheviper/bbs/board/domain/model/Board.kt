package com.retheviper.bbs.board.domain.model

import com.retheviper.bbs.common.value.BoardId

data class Board(
    val id: BoardId,
    val name: String,
    val description: String?
)