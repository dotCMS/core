const path = require('path');
const serverDistPath = path.join(process.cwd(), 'dist/angular-ssr/server/main.server.mjs');
module.exports = require(serverDistPath).default;
