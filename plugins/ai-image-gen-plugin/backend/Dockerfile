FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY server.js ./

EXPOSE 3001

ENV PORT=3001
ENV MOCK_IMAGE_URL=https://picsum.photos

CMD ["node", "server.js"]