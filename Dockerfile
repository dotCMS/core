FROM node:10.15.3

WORKDIR /usr/src/app

# ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update \
    && apt-get install -y --no-install-recommends chromium

ENV CHROME_BIN=chromium

COPY package*.json ./

RUN npm install

COPY . .

# CMD ["npm", "start"]
