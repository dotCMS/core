export default [
    {
        context: [
            '/api',
            '/c/portal',
            '/html',
            '/dwr',
            '/dA',
            '/dotcms-webcomponents',
            '/DotAjaxDirector',
            '/contentAsset',
            '/application',
            '/assets',
            '/dotcms-block-editor'
        ],
        target: 'http://localhost:8080',
        secure: false,
        logLevel: 'debug',
        pathRewrite: {
            '^/assets': '/dotAdmin/assets'
        }
    }
];
