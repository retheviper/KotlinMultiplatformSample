package com.retheviper.bbs.board.route

import com.retheviper.bbs.constant.BOARD
import io.ktor.server.routing.Route
import io.ktor.server.routing.route

fun Route.routeBoard() {
    route(BOARD) {
        routeArticle()
        routeComment()
    }
}