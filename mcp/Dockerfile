FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
RUN npm run build

# To ensure the MCP protocol can gracefully stay open as a "server" waiting for stdin
CMD ["node", "dist/index.js"]
