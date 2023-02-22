package com.retheviper.bbs.testing

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import org.instancio.Instancio
import org.instancio.Select.field

object TestModelFactory {

    fun userModel(): User {
        return Instancio.create(User::class.java)
    }

    fun articleModel(authorId: UserId): Article {
        return Instancio.of(Article::class.java)
            .ignore(field(Article::class.java, "id"))
            .set(field(Article::class.java, "authorId"), authorId.value)
            .create()
    }

    fun articleRecordModel(id: ArticleId, authorId: UserId): ArticleRecord {
        return Instancio.of(ArticleRecord::class.java)
            .set(field(ArticleRecord::class.java, "id"), id.value)
            .set(field(ArticleRecord::class.java, "authorId"), authorId.value)
            .create()
    }

    fun commentModel(articleId: ArticleId, authorId: UserId): Comment {
        return Instancio.of(Comment::class.java)
            .ignore(field(Comment::class.java, "id"))
            .set(field(Comment::class.java, "articleId"), articleId.value)
            .set(field(Comment::class.java, "authorId"), authorId.value)
            .create()
    }

    fun commentRecordModel(id: CommentId, articleId: ArticleId, authorId: UserId): CommentRecord {
        return Instancio.of(CommentRecord::class.java)
            .set(field(CommentRecord::class.java, "id"), id)
            .set(field(CommentRecord::class.java, "articleId"), articleId.value)
            .set(field(CommentRecord::class.java, "authorId"), authorId.value)
            .create()
    }
}