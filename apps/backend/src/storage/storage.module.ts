import { Module } from '@nestjs/common';
import { LocalStorageAdapter } from './local-storage.adapter';
import { STORAGE_ADAPTER } from './storage.adapter';

@Module({
  providers: [{ provide: STORAGE_ADAPTER, useClass: LocalStorageAdapter }],
  exports: [STORAGE_ADAPTER],
})
export class StorageModule {}
