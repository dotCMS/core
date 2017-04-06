/**
 * @author: dotcms
 */

const helpers = require('./helpers');
const webpackMerge = require('webpack-merge'); // used to merge webpack configs
const prodConfig = require('./webpack.prod.js'); // the settings that are common to prod and dev
const GenerateJsonPlugin = require('generate-json-webpack-plugin');
var packageJson = require('../package.json');

const ENV = process.env.NODE_ENV = process.env.ENV = 'production';
const HOST = process.env.HOST || 'localhost';
const PORT = process.env.PORT || 8080;
const METADATA = webpackMerge(prodConfig({
  env: ENV
}).metadata, {
  host: HOST,
  port: PORT,
  ENV: ENV,
  HMR: false,
  DEFAULT_LOCALE: 'en-US',
  baseUrl: '/'
});

module.exports = function (env) {
  return webpackMerge(prodConfig({
    env: ENV
  }), {
  plugins: [
    new GenerateJsonPlugin('package.json', {
      name: packageJson.name,
      version: `${packageJson.version}-${new Date().getTime()}`,
      license: packageJson.license,
      author: packageJson.author,
      description: packageJson.description,
      repository: packageJson.repository,
      engines: packageJson.engines,
    }, 0, 2)
  ]
  });
}