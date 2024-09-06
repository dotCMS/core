const fs = require('fs');
const path = require('path');

const srcDir = path.join(__dirname, '../../../dist/apps/dotcms-block-editor');
const jsOutputFile = path.join(
    __dirname,
    '../../../../dotCMS/src/main/webapp/html/dotcms-block-editor.js'
);
const cssOutputFile = path.join(
    __dirname,
    '../../../../dotCMS/src/main/webapp/html/css/dotcms-block-editor.css'
);

const specificJsFiles = ['generator-runtime', 'main', 'polyfills'];
const cssFilePattern = 'styles.css';

function concatenateFiles(outputFilePath, filePatterns, includeChunks = false) {
    let content = '';
    const files = fs.readdirSync(srcDir);

    filePatterns.forEach((pattern) => {
        const matchedFile = files.find((file) => file.startsWith(pattern));
        if (matchedFile) {
            content += fs.readFileSync(path.join(srcDir, matchedFile), 'utf-8') + '\n';
        }
    });

    if (includeChunks) {
        files
            .filter((file) => file.startsWith('chunk-'))
            .forEach((chunkFile) => {
                content += fs.readFileSync(path.join(srcDir, chunkFile), 'utf-8') + '\n';
            });
    }

    fs.writeFileSync(outputFilePath, content);
}

concatenateFiles(jsOutputFile, specificJsFiles, true);
concatenateFiles(cssOutputFile, [cssFilePattern]);

console.log('Block editor files concatenated successfully!');
