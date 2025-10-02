const path = require('path');

module.exports = async (req, res) => {
  const serverDistPath = path.join(process.cwd(), 'dist/angular-ssr/server/server.mjs');
  const { default: app } = await import(serverDistPath);
  return app(req, res);
};
