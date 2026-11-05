import { dataUrlToFile, isDataUrl } from './data-url.utils';

describe('isDataUrl', () => {
    it('recognises data URLs regardless of case', () => {
        expect(isDataUrl('data:image/png;base64,AAAA')).toBe(true);
        expect(isDataUrl('DATA:image/png;base64,AAAA')).toBe(true);
    });

    it('rejects everything else', () => {
        expect(isDataUrl('https://dotcms.com/a.png')).toBe(false);
        expect(isDataUrl('/dA/123')).toBe(false);
        expect(isDataUrl('')).toBe(false);
    });
});

describe('dataUrlToFile', () => {
    it('decodes a base64 image into a File with the mime type and a matching extension', () => {
        // 8 raw bytes: the PNG signature.
        const file = dataUrlToFile('data:image/png;base64,iVBORw0KGgo=', 'pasted-image');

        expect(file).not.toBeNull();
        expect(file?.name).toBe('pasted-image.png');
        expect(file?.type).toBe('image/png');
        expect(file?.size).toBe(8);
    });

    it('decodes a percent-encoded (non-base64) data URL', () => {
        const file = dataUrlToFile('data:image/svg+xml,%3Csvg%3E%3C%2Fsvg%3E', 'pasted-image');

        expect(file?.type).toBe('image/svg+xml');
        expect(file?.name).toBe('pasted-image.svg');
        expect(file?.size).toBe('<svg></svg>'.length);
    });

    it('keeps the name when it already carries an extension', () => {
        const file = dataUrlToFile('data:image/jpeg;base64,/9j/4A==', 'photo.jpg');

        expect(file?.name).toBe('photo.jpg');
        expect(file?.type).toBe('image/jpeg');
    });

    it('returns null for non-image or malformed data URLs', () => {
        expect(dataUrlToFile('data:text/plain;base64,aGk=', 'x')).toBeNull();
        expect(dataUrlToFile('https://dotcms.com/a.png', 'x')).toBeNull();
        expect(dataUrlToFile('data:image/png;base64', 'x')).toBeNull();
        expect(dataUrlToFile('data:image/png;base64,***', 'x')).toBeNull();
    });
});
