package com.retheviper.bbs.common.exception

import com.retheviper.bbs.constant.ErrorCode

open class BadRequestException(message: String, val code: ErrorCode = ErrorCode.INVALID_PARAMETER) :
    RuntimeException(message)

open class NotFoundException(message: String, code: ErrorCode) : BadRequestException(message, code)

class UserNotFoundException(message: String) : NotFoundException(message, ErrorCode.USER_NOT_FOUND)

class UserAlreadyExistsException(message: String) : BadRequestException(message, ErrorCode.USER_ALREADY_EXISTS)

class PasswordNotMatchException(message: String, code: ErrorCode) : BadRequestException(message, code)

class InvalidTokenException(message: String) : BadRequestException(message, ErrorCode.INVALID_TOKEN)

class BoardNotFoundException(message: String) : NotFoundException(message, ErrorCode.BOARD_NOT_FOUND)

class ArticleNotFoundException(message: String) : NotFoundException(message, ErrorCode.ARTICLE_NOT_FOUND)

class CommentNotFoundException(message: String) : NotFoundException(message, ErrorCode.COMMENT_NOT_FOUND)