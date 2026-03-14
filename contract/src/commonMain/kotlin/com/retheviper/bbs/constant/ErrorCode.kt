package com.retheviper.bbs.constant

enum class ErrorCode(val value: String) {
    // Common
    UNKNOWN_ERROR("C001"),
    INVALID_PARAMETER("C002"),
    INVALID_TOKEN("C003"),
    AUTHENTICATION_FAILED("C004"),
    ID_NOT_MATCH("C005"),

    // User
    USER_NOT_FOUND("U001"),
    USER_ALREADY_EXISTS("U002"),
    USER_PASSWORD_NOT_MATCH("U003"),

    // Board
    BOARD_NOT_FOUND("B001"),
    BOARD_ALREADY_EXISTS("B002"),

    // Article
    ARTICLE_NOT_FOUND("A001"),
    ARTICLE_ALREADY_EXISTS("A002"),
    ARTICLE_PASSWORD_NOT_MATCH("A003"),
    ARTICLE_AUTHOR_NOT_MATCH("A004"),

    // Category
    CATEGORY_NOT_FOUND("C001"),
    CATEGORY_ALREADY_EXISTS("C002"),

    // Comment
    COMMENT_NOT_FOUND("C001"),
    COMMENT_AUTHOR_NOT_MATCH("C002"),
    COMMENT_PASSWORD_NOT_MATCH("C003");


    fun from(value: String): ErrorCode {
        return values().firstOrNull { it.value == value } ?: UNKNOWN_ERROR
    }
}