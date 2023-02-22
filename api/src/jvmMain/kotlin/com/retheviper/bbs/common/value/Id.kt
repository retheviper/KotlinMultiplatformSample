package com.retheviper.bbs.common.value

@JvmInline
value class ArticleId(val value: Int)

@JvmInline
value class CommentId(val value: Int)

@JvmInline
value class UserId(val value: Int) {
    override fun toString(): String {
        return value.toString()
    }
}
