import { TreeNodeItem } from '@dotcms/dotcms-models';

import { formatFolderSearchPath } from './folder-search-path.utils';

/**
 * The second line of a folder-search result row: `demo.dotcms.com / images / thumbnails`.
 *
 * These cases are the behaviour of the Site/Folder field's `formatSearchNodePath`, which this
 * helper replaces for both consumers. They are pinned here precisely because that field must come
 * out of the extraction rendering identically (spec FR-027 / SC-010) — a change in this table is a
 * change to a shipped surface, not a refactor.
 */
const node = (hostname: string | undefined, path: string | undefined): TreeNodeItem =>
    ({
        key: 'k',
        label: 'folder',
        data: { type: 'folder', id: 'f1', hostname, path }
    }) as TreeNodeItem;

describe('formatFolderSearchPath', () => {
    it('renders the hostname alone for the site root', () => {
        expect(formatFolderSearchPath(node('//demo.dotcms.com', '/'))).toBe('demo.dotcms.com');
    });

    it('renders the hostname alone when the path is absent', () => {
        expect(formatFolderSearchPath(node('//demo.dotcms.com', undefined))).toBe(
            'demo.dotcms.com'
        );
    });

    it('joins a single segment with the hostname', () => {
        expect(formatFolderSearchPath(node('//demo.dotcms.com', '/activities/'))).toBe(
            'demo.dotcms.com / activities'
        );
    });

    it('joins every segment of a nested path in order', () => {
        expect(formatFolderSearchPath(node('//demo.dotcms.com', '/images/thumbnails/'))).toBe(
            'demo.dotcms.com / images / thumbnails'
        );
    });

    it('strips the leading double slash from the hostname', () => {
        expect(formatFolderSearchPath(node('//blog.dotcms.com', '/posts/'))).toBe(
            'blog.dotcms.com / posts'
        );
    });

    it('accepts a hostname that has no leading slashes', () => {
        expect(formatFolderSearchPath(node('demo.dotcms.com', '/posts/'))).toBe(
            'demo.dotcms.com / posts'
        );
    });

    it('drops empty segments produced by repeated or stray slashes', () => {
        expect(formatFolderSearchPath(node('//demo.dotcms.com', '//images///thumbs//'))).toBe(
            'demo.dotcms.com / images / thumbs'
        );
    });

    it('renders the segments alone when the hostname is missing', () => {
        expect(formatFolderSearchPath(node(undefined, '/images/thumbnails/'))).toBe(
            'images / thumbnails'
        );
    });

    it('renders an empty string when there is neither hostname nor path', () => {
        expect(formatFolderSearchPath(node(undefined, undefined))).toBe('');
    });
});
