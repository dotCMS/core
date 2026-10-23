import { isImageUrl, isValidHttpUrl, isValidLinkHref } from './url.utils';

describe('isValidHttpUrl', () => {
    it('accepts http and https', () => {
        expect(isValidHttpUrl('https://dotcms.com')).toBe(true);
        expect(isValidHttpUrl('http://dotcms.com/a')).toBe(true);
    });

    it('rejects other schemes and plain text', () => {
        expect(isValidHttpUrl('mailto:a@b.c')).toBe(false);
        expect(isValidHttpUrl('dotcms')).toBe(false);
    });
});

describe('isValidLinkHref', () => {
    it('accepts anchors, relative paths, mailto and tel', () => {
        expect(isValidLinkHref('#top')).toBe(true);
        expect(isValidLinkHref('/page')).toBe(true);
        expect(isValidLinkHref('../page')).toBe(true);
        expect(isValidLinkHref('mailto:a@b.c')).toBe(true);
        expect(isValidLinkHref('tel:+1555')).toBe(true);
    });

    it('rejects empty and unknown schemes', () => {
        expect(isValidLinkHref('   ')).toBe(false);
        expect(isValidLinkHref('ftp://x')).toBe(false);
    });
});

/**
 * The legacy block-editor turned a pasted image URL into an image node (its `isImageURL`); the new
 * editor must do the same for parity (#38165).
 */
describe('isImageUrl', () => {
    it('accepts an http(s) URL ending in an image extension', () => {
        expect(isImageUrl('https://dotcms.com/images/logo.png')).toBe(true);
        expect(isImageUrl('http://dotcms.com/a/b.JPG')).toBe(true);
        expect(isImageUrl('https://dotcms.com/a.webp')).toBe(true);
        expect(isImageUrl('https://dotcms.com/a.svg')).toBe(true);
    });

    it('tolerates a query string and surrounding whitespace', () => {
        expect(isImageUrl('https://dotcms.com/a.png?w=100')).toBe(true);
        expect(isImageUrl('  https://dotcms.com/a.gif\n')).toBe(true);
    });

    it('rejects non-image URLs, data URLs and plain text', () => {
        expect(isImageUrl('https://dotcms.com/page')).toBe(false);
        expect(isImageUrl('https://dotcms.com/a.pdf')).toBe(false);
        expect(isImageUrl('data:image/png;base64,iVBORw0KGgo=')).toBe(false);
        expect(isImageUrl('a.png')).toBe(false);
        expect(isImageUrl('some text with https://dotcms.com/a.png inside')).toBe(false);
    });
});
