var path = require('path');
var webpack = require('webpack');
var WebpackBuildNotifierPlugin = require('webpack-build-notifier');
var CommonsChunkPlugin = webpack.optimize.CommonsChunkPlugin;
// const ExtractTextPlugin = require('extract-text-webpack-plugin')

const PATHS = {
    src: path.join(__dirname, '../src'),
    build: path.join(__dirname, '../build-dotcms-js')
}

module.exports = {

    // entry: {
    //     'dotcms-js': PATHS.src + '/main.browser.ts'
    // },
    entry: {
        '@angular': [
            'rxjs',
            'reflect-metadata',
            'zone.js'
        ],
        'common': ['es6-shim'],
        // 'app': './src/app/main.ts',
        // 'vendor': './showcase/vendor.ts'
    },
    output: {
        path: PATHS.build,
        filename: '[name].js',
        library: 'dotcms-js',
        libraryTarget: 'umd'
    },
    devtool: 'source-map',
    module: {
        rules: [
            {
                test: /\.ts$/,
                loader: 'awesome-typescript-loader',
                options:
                    {
                        configFileName: 'tsconfig.dotcms-js.json',
                    }
            },
            {
                test: /\.(html)$/,
                loader: 'raw-loader'
            },
            {
                test: /\.json$/,
                loader: 'json-loader',
            },
            {
                test: /\.(jpg|png|gif)$/,
                use: 'file-loader?name=assets/img/[name]-[hash].[ext]'
            },
            {
                test: /\.scss$/,
                use: ['to-string-loader', 'css-loader', 'sass-loader'],
            },
            {
                test: /\.css$/,
                use: ['to-string-loader', 'css-loader'],
            },
            {
                test: /\.(eot|woff2?|svg|ttf)([\?]?.*)$/,
                use: 'file-loader?name=assets/fonts/[name]-[hash].[ext]'
            }
        ],
    },
    resolve: {
        // you can now require('file') instead of require('file.js')
        extensions: ['.ts', '.js', '.json', '.css', '.html']
    },
    plugins: [
        new CommonsChunkPlugin({names: ['@angular', 'common'], minChunks: Infinity}),
        new WebpackBuildNotifierPlugin({
            title: 'My Project Webpack Build'
        }),
        // new ExtractTextPlugin('solar-popup.css'),
        new webpack.IgnorePlugin(/test\.spec\.ts$/)


    ]
}
