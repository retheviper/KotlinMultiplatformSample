package com.retheviper.bbs.board.domain.service

import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.board.infrastructure.BoardRepository
import com.retheviper.bbs.common.exception.BoardNotFoundException
import org.jetbrains.exposed.sql.transactions.transaction

class BoardService(private val repository: BoardRepository) {

    fun findAll(page: Int, pageSize: Int, limit: Int): List<Board> {
        return transaction {
            repository.findAll(
                page = page,
                pageSize = pageSize,
                limit = limit
            )
        }
    }

    fun find(id: Int): Board {
        return transaction {
            repository.find(id)
        } ?: throw BoardNotFoundException("Board not found with id: $id.")
    }

    fun create(board: Board) {
        transaction {
            repository.create(board)
        }
    }
}