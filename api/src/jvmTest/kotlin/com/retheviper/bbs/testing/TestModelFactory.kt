package com.retheviper.bbs.testing

import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.domain.model.Tag
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.board.infrastructure.model.CategoryRecord
import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.user.domain.model.User
import org.instancio.Instancio
import org.instancio.Select.field

object TestModelFactory {

    fun userModel(): User {
        return Instancio.create(User::class.java)
    }

    fun articleModel(authorId: UserId, category: Category): Article {
        return Instancio.of(Article::class.java)
            .ignore(field("id"))
            .set(field("authorId"), authorId.value)
            .set(field("category"), category)
            .create()
    }

    fun articleRecordModel(id: ArticleId, categoryId: CategoryId, authorId: UserId): ArticleRecord {
        return Instancio.of(ArticleRecord::class.java)
            .set(field("id"), id.value)
            .set(field("categoryId"), categoryId)
            .set(field( "authorId"), authorId.value)
            .create()
    }

    fun categoryModel(): Category {
        return Instancio.of(Category::class.java)
            .ignore(field("id"))
            .create()
    }

    fun categoryRecordModel(id: CategoryId): CategoryRecord {
        return Instancio.of(CategoryRecord::class.java)
            .set(field("id"), id.value)
            .create()
    }

    fun tagModel(articleId: ArticleId): Tag {
        return Instancio.of(Tag::class.java)
            .ignore(field("id"))
            .set(field("articleId"), articleId)
            .create()
    }

    fun tagModels(articleId: ArticleId, count: Int = 10): List<Tag> {
        return Instancio.ofList(Tag::class.java)
            .size(count)
            .ignore(field(Tag::class.java, "id"))
            .set(field(Tag::class.java, "articleId"), articleId)
            .create()
    }

    fun commentModel(articleId: ArticleId, authorId: UserId): Comment {
        return Instancio.of(Comment::class.java)
            .ignore(field("id"))
            .set(field("articleId"), articleId.value)
            .set(field("authorId"), authorId.value)
            .create()
    }

    fun commentModels(articleId: ArticleId, authorId: UserId, count: Int = 10): List<Comment> {
        return Instancio.ofList(Comment::class.java)
            .size(count)
            .ignore(field(Comment::class.java, "id"))
            .set(field(Comment::class.java, "articleId"), articleId.value)
            .set(field(Comment::class.java, "authorId"), authorId.value)
            .create()
    }

    fun commentRecordModel(id: CommentId, articleId: ArticleId, authorId: UserId): CommentRecord {
        return Instancio.of(CommentRecord::class.java)
            .set(field("id"), id)
            .set(field("articleId"), articleId.value)
            .set(field("authorId"), authorId.value)
            .create()
    }

    fun commentRecordModels(articleId: ArticleId, authorId: UserId, count: Int = 10): List<CommentRecord> {
        return Instancio.ofList(CommentRecord::class.java)
            .size(count)
            .ignore(field(CommentRecord::class.java, "id"))
            .set(field(CommentRecord::class.java, "articleId"), articleId.value)
            .set(field(CommentRecord::class.java, "authorId"), authorId.value)
            .create()
    }
}