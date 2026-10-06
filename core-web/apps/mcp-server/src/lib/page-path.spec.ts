import { normalizePagePath } from './page-path';

describe('normalizePagePath', () => {
    it('leaves an already-clean path alone', () => {
        expect(normalizePagePath('/about-us')).toBe('/about-us');
        expect(normalizePagePath('/blog/post/hello')).toBe('/blog/post/hello');
    });

    it('adds the leading slash', () => {
        expect(normalizePagePath('about-us')).toBe('/about-us');
    });

    it('collapses . and .. segments', () => {
        // Live bug before this: `page_verify({ path: '/a/../b' })` RENDERED /b while the
        // manifest reported '/a/../b' — the tool reporting on a page it did not read.
        expect(normalizePagePath('/a/../b')).toBe('/b');
        expect(normalizePagePath('/a/./b')).toBe('/a/b');
        expect(normalizePagePath('/a/b/../../c')).toBe('/c');
    });

    it('cannot escape above the root', () => {
        // Latent until an `allow` policy exists: requestCore policy-checks the RAW string and
        // normalizes afterwards, so this would pass an `/api/v1/page/` prefix allowlist and
        // then resolve somewhere else entirely.
        expect(normalizePagePath('/../../../../api/v1/users/current')).toBe(
            '/api/v1/users/current'
        );
    });

    it('drops a query string and a fragment', () => {
        // A `#` used to silently truncate the path with no mention in the manifest.
        expect(normalizePagePath('/about-us?foo=1')).toBe('/about-us');
        expect(normalizePagePath('/about-us#section')).toBe('/about-us');
    });

    it('normalizes the root to /', () => {
        expect(normalizePagePath('/')).toBe('/');
        expect(normalizePagePath('///')).toBe('/');
    });

    it('collapses repeated slashes', () => {
        expect(normalizePagePath('/a//b')).toBe('/a/b');
    });

    it('does not read a leading // as a host', () => {
        // To the URL API a `//` prefix is scheme-relative, so `//books/index` parses `books`
        // as a HOST and yields `/index` — a different page, with nothing to indicate it.
        // Callers reach this legitimately: `//host/path` is how dotCMS writes a
        // host-qualified path elsewhere.
        expect(normalizePagePath('//books/index')).toBe('/books/index');
    });

    it('keeps a space encoded so the result is a usable URL path', () => {
        expect(normalizePagePath('/my%20books')).toBe('/my%20books');
        expect(normalizePagePath('/my books')).toBe('/my%20books');
    });

    it('refuses an encoded path separator', () => {
        // Splitting first does not make it inert — callers rebuild by joining segments, which
        // turns it straight back into a path boundary.
        expect(() => normalizePagePath('/a/my%2Fbooks')).toThrow(/encoded path separator/i);
    });

    it('rejects an empty path', () => {
        expect(() => normalizePagePath('')).toThrow(/must not be empty/i);
        expect(() => normalizePagePath('   ')).toThrow(/must not be empty/i);
    });

    it('uses the caller-supplied label in its errors', () => {
        expect(() => normalizePagePath('', 'urlPath')).toThrow(/urlPath must not be empty/i);
    });
});
