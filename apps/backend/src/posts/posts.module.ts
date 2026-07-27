import { Module } from '@nestjs/common';
import { MongooseModule } from '@nestjs/mongoose';
import { AuthModule } from '../auth/auth.module';
import { StorageModule } from '../storage/storage.module';
import { Post, PostSchema } from './schemas/post.schema';
import { PostRepository } from './post.repository';
import { PostsService } from './posts.service';
import { PostsController } from './posts.controller';

@Module({
  imports: [
    MongooseModule.forFeature([{ name: Post.name, schema: PostSchema }]),
    AuthModule,
    StorageModule,
  ],
  providers: [PostRepository, PostsService],
  controllers: [PostsController],
})
export class PostsModule {}
