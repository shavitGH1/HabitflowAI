import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { StorageModule } from '../storage/storage.module';
import { FollowsModule } from '../follows/follows.module';
import { Post, PostSchema } from './schemas/post.schema';
import { Comment, CommentSchema } from './schemas/comment.schema';
import { PostRepository } from './post.repository';
import { CommentRepository } from './comment.repository';
import { PostsService } from './posts.service';
import { CommentsService } from './comments.service';
import { PostsController } from './posts.controller';
import { CommentsController } from './comments.controller';

@Module({
  imports: [
    MongooseModule.forFeature([
      { name: Post.name, schema: PostSchema },
      { name: Comment.name, schema: CommentSchema },
    ]),
    AuthModule,
    StorageModule,
    FollowsModule,
  ],
  providers: [PostRepository, CommentRepository, PostsService, CommentsService],
  controllers: [PostsController, CommentsController],
})
export class PostsModule {}
