package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Boards
import com.retheviper.bbs.common.value.BoardId
import org.jetbrains.exposed.sql.insertAndGetId

class BoardRepository {

    fun create(board: Board): BoardId {
        val id = Boards.insertAndGetId {
            it[name] = board.name
            it[description] = board.description
            insertAuditInfos(it, "system")
        }.value

        return BoardId(id)
    }
}