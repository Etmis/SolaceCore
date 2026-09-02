FROM node:20-alpine

WORKDIR /app

# Install dependencies
COPY package*.json ./
RUN npm ci --omit=dev

# Copy source code and TypeScript project config files needed for the build
COPY server ./server
COPY src ./src
COPY public ./public
COPY index.html tsconfig.json tsconfig.app.json tsconfig.node.json vite.config.ts ./

# Install build dependencies temporarily to build frontend
RUN npm ci

# Build frontend
RUN npm run build

# Remove build dependencies to keep image small
RUN npm prune --omit=dev

ENV NODE_ENV=production

EXPOSE 3001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD node -e "require('http').get('http://localhost:3001/api/health', (r) => {if (r.statusCode !== 200) throw new Error(r.statusCode)})"

CMD ["node", "server/index.js"]