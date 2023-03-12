package com.retheviper.bbs.common.value

interface Id {
    val value: Int
}

@JvmInline
value class BoardId(override val value: Int): Id {
    override fun toString() = value.toString()
}

@JvmInline
value class ArticleId(override val value: Int): Id {
    override fun toString() = value.toString()
}

@JvmInline
value class CommentId(override val value: Int): Id {
    override fun toString() = value.toString()
}

@JvmInline
value class UserId(override val value: Int): Id {
    override fun toString() = value.toString()
}

@JvmInline
value class CategoryId(override val value: Int): Id {
    override fun toString() = value.toString()
}

@JvmInline
value class TagId(override val value: Int): Id {
    override fun toString() = value.toString()
}