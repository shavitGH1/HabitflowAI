import * as fs from 'fs';
import * as path from 'path';
import { Injectable } from '@nestjs/common';
import { v4 as uuidv4 } from 'uuid';
import { IStorageAdapter } from './storage.adapter';

@Injectable()
export class LocalStorageAdapter implements IStorageAdapter {
  private readonly uploadsDir = path.join(process.cwd(), 'uploads');

  constructor() {
    if (!fs.existsSync(this.uploadsDir)) {
      fs.mkdirSync(this.uploadsDir, { recursive: true });
    }
  }

  async upload(file: Express.Multer.File): Promise<string> {
    const ext = path.extname(file.originalname);
    const filename = `${uuidv4()}${ext}`;
    fs.writeFileSync(path.join(this.uploadsDir, filename), file.buffer);
    return `/uploads/${filename}`;
  }

  async delete(url: string): Promise<void> {
    const filename = path.basename(url);
    const filePath = path.join(this.uploadsDir, filename);
    if (fs.existsSync(filePath)) {
      fs.unlinkSync(filePath);
    }
  }
}
