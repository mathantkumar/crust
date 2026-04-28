FROM node:20-alpine
WORKDIR /app
COPY package*.json ./
RUN npm install
COPY . .
# We just use npm run dev as a quick bridge in compose or we can build
RUN npm run build
EXPOSE 3000
CMD ["npm", "start"]
