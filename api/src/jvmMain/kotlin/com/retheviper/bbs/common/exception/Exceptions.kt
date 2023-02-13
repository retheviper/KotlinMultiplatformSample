package com.retheviper.bbs.common.exception

import com.retheviper.bbs.constant.ErrorCode

open class BadRequestException(message: String, val code: ErrorCode = ErrorCode.INVALID_PARAMETER) :
    RuntimeException(message)

class UserNotFoundException(message: String) : BadRequestException(message, ErrorCode.USER_NOT_FOUND)

class UserAlreadyExistsException(message: String) : BadRequestException(message, ErrorCode.USER_ALREADY_EXISTS)

class PasswordNotMatchException(message: String, code: ErrorCode) : BadRequestException(message, code)

class InvalidTokenException(message: String) : BadRequestException(message, ErrorCode.INVALID_TOKEN)

class BoardNotFoundException(message: String) : BadRequestException(message, ErrorCode.BOARD_NOT_FOUND)

class ArticleNotFoundException(message: String) : BadRequestException(message, ErrorCode.ARTICLE_NOT_FOUND)

class CommentNotFoundException(message: String) : BadRequestException(message, ErrorCode.COMMENT_NOT_FOUND)