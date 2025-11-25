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
            '/JSONTags',
            '/api/vtl',
            '/tinymce'
        ],
        target: 'http://localhost:8080',
        secure: false,
        logLevel: 'debug',
        pathRewrite: {
            '^/assets/manifest.json': '/dotAdmin/assets/manifest.json',
            '^/assets/monaco-editor/min': '/dotAdmin/assets/monaco-editor/min',
            '^/assets': '/dotAdmin',
            '^/tinymce': '/dotAdmin/tinymce'
        }
    }
];
