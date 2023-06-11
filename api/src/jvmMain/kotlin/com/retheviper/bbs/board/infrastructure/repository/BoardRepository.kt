package com.retheviper.bbs.board.infrastructure.repository

import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.board.infrastructure.model.BoardRecord
import com.retheviper.bbs.common.extension.insertAuditInfos
import com.retheviper.bbs.common.extension.updateAuditInfos
import com.retheviper.bbs.common.infrastructure.table.Boards
import com.retheviper.bbs.common.value.BoardId
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insertAndGetId
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.update

class BoardRepository {

    fun findAll(): List<BoardRecord> {
        return Boards.selectAll().map { it.toRecord() }
    }

    fun find(id: BoardId): BoardRecord? {
        return Boards.select { Boards.id eq id.value }
            .firstOrNull()
            ?.toRecord()
    }

    fun create(board: Board): Board {
        val id = Boards.insertAndGetId {
            it[name] = board.name
            it[description] = board.description
            insertAuditInfos(it, "system")
        }.value

        return board.copy(id = BoardId(id))
    }

    fun update(board: Board) {
        Boards.update({ Boards.id eq board.id.value }) {
            it[name] = board.name
            it[description] = board.description
            updateAuditInfos(it, "system")
        }
    }

    fun delete(id: BoardId) {
        Boards.deleteWhere { Boards.id eq id.value }
    }

    private fun ResultRow.toRecord() = BoardRecord(
        id = BoardId(this[Boards.id].value),
        name = this[Boards.name],
        description = this[Boards.description]
    )
}