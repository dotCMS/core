import { describe, expect, it } from 'vitest';

import { createDotCMSImageLoader } from './imageLoader';

describe('createDotCMSImageLoader', () => {
    it('builds an absolute /dA URL with width, quality and language defaults', () => {
        const loader = createDotCMSImageLoader('https://demo.dotcms.com');

        expect(loader('abc123', { width: 800 })).toBe(
            'https://demo.dotcms.com/dA/abc123/800w/50q?language_id=1'
        );
    });

    it('omits the width segment when no width is given', () => {
        const loader = createDotCMSImageLoader('https://demo.dotcms.com');

        expect(loader('abc123')).toBe('https://demo.dotcms.com/dA/abc123/50q?language_id=1');
    });

    it('honors custom quality and languageId', () => {
        const loader = createDotCMSImageLoader('https://demo.dotcms.com');

        expect(loader('abc123', { width: 400, quality: 90, languageId: '2' })).toBe(
            'https://demo.dotcms.com/dA/abc123/400w/90q?language_id=2'
        );
    });

    it('produces site-relative URLs when no host is provided (dev proxy)', () => {
        const loader = createDotCMSImageLoader();

        expect(loader('abc123', { width: 800 })).toBe('/dA/abc123/800w/50q?language_id=1');
    });

    it('does not double-prefix a src that already contains /dA/', () => {
        const loader = createDotCMSImageLoader('https://demo.dotcms.com');

        expect(loader('/dA/abc123', { width: 800 })).toBe(
            'https://demo.dotcms.com/dA/abc123/800w/50q?language_id=1'
        );
    });

    it('returns absolute http(s) URLs unchanged', () => {
        const loader = createDotCMSImageLoader('https://demo.dotcms.com');

        expect(loader('https://images.unsplash.com/photo.jpg', { width: 800 })).toBe(
            'https://images.unsplash.com/photo.jpg'
        );
    });

    it('uses only the origin of the given host, discarding any path', () => {
        const loader = createDotCMSImageLoader('https://demo.dotcms.com/some/path');

        expect(loader('abc123')).toBe('https://demo.dotcms.com/dA/abc123/50q?language_id=1');
    });

    it('throws on a non-empty invalid host', () => {
        expect(() => createDotCMSImageLoader('not a url')).toThrow();
    });
});
