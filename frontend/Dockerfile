# Stage 1: Build
FROM node:20-alpine AS builder
WORKDIR /app

# Accept env vars as build args (Vite bakes them in at build time)
ARG VITE_API_BASE_URL=http://localhost:8080
ARG VITE_PDF_SERVICE_URL=http://localhost:8000
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL
ENV VITE_PDF_SERVICE_URL=$VITE_PDF_SERVICE_URL

COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# Stage 2: Serve static bundle with Nginx
FROM nginx:alpine AS runner
COPY --from=builder /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
