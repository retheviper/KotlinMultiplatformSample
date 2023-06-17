package com.retheviper.bbs.testing

import com.retheviper.bbs.auth.domain.model.Credential
import com.retheviper.bbs.board.domain.model.Article
import com.retheviper.bbs.board.domain.model.Board
import com.retheviper.bbs.board.domain.model.Category
import com.retheviper.bbs.board.domain.model.Comment
import com.retheviper.bbs.board.domain.model.Tag
import com.retheviper.bbs.board.infrastructure.model.ArticleRecord
import com.retheviper.bbs.board.infrastructure.model.CategoryRecord
import com.retheviper.bbs.board.infrastructure.model.CommentRecord
import com.retheviper.bbs.board.infrastructure.model.TagRecord
import com.retheviper.bbs.common.extension.toHashedString
import com.retheviper.bbs.common.value.ArticleId
import com.retheviper.bbs.common.value.BoardId
import com.retheviper.bbs.common.value.CategoryId
import com.retheviper.bbs.common.value.CommentId
import com.retheviper.bbs.common.value.TagId
import com.retheviper.bbs.common.value.UserId
import com.retheviper.bbs.model.common.PaginationProperties
import com.retheviper.bbs.user.domain.model.User
import org.instancio.Instancio
import org.instancio.Select.field

object TestModelFactory {

    fun paginationPropertiesModel(): PaginationProperties {
        return PaginationProperties(1, 10, 100)
    }

    fun userModel(): User {
        return Instancio.create(User::class.java)
    }

    fun credentialModel(userId: UserId = UserId(1)): Credential {
        return Instancio.of(Credential::class.java)
            .set(field("userId"), userId)
            .create()
    }

    fun boardModel(): Board {
        return Instancio.create(Board::class.java)
    }

    fun articleModel(boardId: BoardId = BoardId(1), authorId: UserId = UserId(1), category: Category): Article {
        return Instancio.of(Article::class.java)
            .ignore(field("id"))
            .set(field("boardId"), boardId)
            .set(field("authorId"), authorId.value)
            .set(field("category"), category)
            .create()
    }

    fun articleRecordModel(
        boardId: BoardId = BoardId(1),
        id: ArticleId = ArticleId(1),
        categoryId: CategoryId = CategoryId(1),
        authorId: UserId = UserId(1)
    ): ArticleRecord {
        return Instancio.of(ArticleRecord::class.java)
            .set(field("boardId"), boardId.value)
            .set(field("id"), id.value)
            .set(field("categoryId"), categoryId)
            .set(field("authorId"), authorId.value)
            .create()
    }

    fun categoryModel(boardId: BoardId = BoardId(1)): Category {
        return Instancio.of(Category::class.java)
            .ignore(field("id"))
            .set(field("boardId"), boardId)
            .create()
    }

    fun categoryRecordModel(id: CategoryId = CategoryId(1)): CategoryRecord {
        return Instancio.of(CategoryRecord::class.java)
            .set(field("id"), id.value)
            .create()
    }

    fun tagModel(articleId: ArticleId = ArticleId(1)): Tag {
        return Instancio.of(Tag::class.java)
            .ignore(field("id"))
            .set(field("articleId"), articleId)
            .create()
    }

    fun tagModels(articleId: ArticleId = ArticleId(1), count: Int = 10): List<Tag> {
        return Instancio.ofList(Tag::class.java)
            .size(count)
            .ignore(field(Tag::class.java, "id"))
            .set(field(Tag::class.java, "articleId"), articleId)
            .create()
    }

    fun tagRecordModel(
        id: TagId = TagId(1),
        articleId: ArticleId = ArticleId(1)
    ): TagRecord {
        return Instancio.of(TagRecord::class.java)
            .set(field("id"), id.value)
            .set(field("articleId"), articleId)
            .create()
    }

    fun tagRecordModels(
        articleId: ArticleId = ArticleId(1),
        count: Int = 10
    ): List<TagRecord> {
        return Instancio.ofList(TagRecord::class.java)
            .size(count)
            .ignore(field(TagRecord::class.java, "id"))
            .set(field(TagRecord::class.java, "articleId"), articleId)
            .create()
    }

    fun commentModel(articleId: ArticleId = ArticleId(1), authorId: UserId = UserId(1)): Comment {
        return Instancio.of(Comment::class.java)
            .ignore(field("id"))
            .set(field("articleId"), articleId.value)
            .set(field("authorId"), authorId.value)
            .create()
    }

    fun commentModels(
        articleId: ArticleId = ArticleId(1),
        authorId: UserId = UserId(1),
        count: Int = 10
    ): List<Comment> {
        return Instancio.ofList(Comment::class.java)
            .size(count)
            .ignore(field(Comment::class.java, "id"))
            .set(field(Comment::class.java, "articleId"), articleId.value)
            .set(field(Comment::class.java, "authorId"), authorId.value)
            .create()
    }

    fun commentRecordModel(
        id: CommentId = CommentId(1),
        articleId: ArticleId = ArticleId(1),
        authorId: UserId = UserId(1)
    ): CommentRecord {
        return Instancio.of(CommentRecord::class.java)
            .set(field("id"), id.value)
            .set(field("articleId"), articleId.value)
            .set(field("authorId"), authorId.value)
            .create()
    }

    fun commentRecordModels(
        articleId: ArticleId = ArticleId(1),
        authorId: UserId = UserId(1),
        count: Int = 10
    ): List<CommentRecord> {
        return Instancio.ofList(CommentRecord::class.java)
            .size(count)
            .ignore(field(CommentRecord::class.java, "id"))
            .set(field(CommentRecord::class.java, "articleId"), articleId.value)
            .set(field(CommentRecord::class.java, "authorId"), authorId.value)
            .create()
    }
}