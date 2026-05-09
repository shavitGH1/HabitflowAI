FROM node:20-alpine

RUN npm install -g pnpm@10.28.2

WORKDIR /app

# Copy workspace root so pnpm resolves onlyBuiltDependencies from root package.json
COPY package.json pnpm-workspace.yaml ./
COPY apps/backend/package.json ./apps/backend/

# Install backend deps within workspace context — no duplicate pnpm config needed
RUN pnpm install --filter=habitflow-ai-backend --no-frozen-lockfile

COPY apps/backend/ ./apps/backend/

WORKDIR /app/apps/backend

EXPOSE 3000

CMD ["pnpm", "run", "start:dev"]
