package com.habitflowai.domain.repository

import com.habitflowai.data.model.Comment
import com.habitflowai.data.model.Post
import kotlinx.coroutines.flow.Flow

interface SocialRepository {
    fun getPosts(page: Int, pageSize: Int): Flow<List<Post>>
    fun getComments(postId: Int): Flow<List<Comment>>
}
