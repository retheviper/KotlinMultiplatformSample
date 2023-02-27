package com.retheviper.bbs.common.value

@JvmInline
value class ArticleId(val value: Int) {
    override fun toString() = value.toString()
}

@JvmInline
value class CommentId(val value: Int) {
    override fun toString() = value.toString()
}

@JvmInline
value class UserId(val value: Int) {
    override fun toString() = value.toString()
}

@JvmInline
value class CategoryId(val value: Int) {
    override fun toString() = value.toString()
}

@JvmInline
value class TagId(val value: Int) {
    override fun toString() = value.toString()
}