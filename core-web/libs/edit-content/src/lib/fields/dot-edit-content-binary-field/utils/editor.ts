export function extractFileExtension(fileName: string): string {
    const includesDot = fileName.includes('.');

    if (!includesDot) {
        return '';
    }

    return fileName.split('.').pop() || '';
}

export function getInfoByLang(extension: string) {
    if (extension === 'vtl') {
        return {
            lang: 'html',
            mimeType: 'text/x-velocity',
            extension: '.vtl'
        };
    }

    const language = monaco.languages
        .getLanguages()
        .find((language) => language.extensions?.includes(`.${extension}`));

    return {
        lang: language?.id || 'text',
        mimeType: language?.mimetypes?.[0] || 'text/plain',
        extension: language?.extensions?.[0] || '.txt'
    };
}
