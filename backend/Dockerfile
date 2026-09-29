# Production Dockerfile for R-7 Calculator AI Backend (Google Cloud Run / Container Platforms)
FROM node:22-alpine AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=8080

COPY package*.json ./

RUN npm ci --only=production

COPY src/ ./src/
COPY server.js ./

# Non-root user for container security
USER node

EXPOSE 8080

CMD ["node", "server.js"]
