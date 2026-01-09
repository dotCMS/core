module.exports = {
    // TypeScript/JavaScript files - lint and format
    '*.{ts,js,tsx,jsx}': (filenames) => {
        // Convert absolute paths to relative paths from core-web directory
        const files = filenames.map((f) => f.replace(/^.*\/core-web\//, '')).join(',');
        return [
            `nx affected -t lint --exclude='tag:skip:lint' --fix --files=${files}`,
            `nx format:write --files=${files}`
        ];
    },

    // JSON, HTML, CSS, SCSS files - format only
    '*.{json,html,css,scss}': (filenames) => {
        const files = filenames.map((f) => f.replace(/^.*\/core-web\//, '')).join(',');
        return `nx format:write --files=${files}`;
    },

    // Always run yarn install if package.json or yarn.lock changes
    'package.json': () => 'yarn install --frozen-lockfile',
    'yarn.lock': () => 'yarn install --frozen-lockfile'
};
