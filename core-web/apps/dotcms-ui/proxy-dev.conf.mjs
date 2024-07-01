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
            '/dotcms-block-editor',
            '/dotcms-binary-field-builder',
            '/categoriesServlet',
            '/JSONTags'
        ],
        target: 'http://localhost:8080',
        secure: false,
        logLevel: 'debug',
        pathRewrite: {
            '^/assets': '/dotAdmin/assets'
        }
    }
];
