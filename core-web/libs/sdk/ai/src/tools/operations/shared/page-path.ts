/**
 * Normalization for the page paths callers hand to the page tools.
 *
 * Shared because all three tools build a request URL by interpolating a caller-supplied path
 * into an endpoint template — `/api/v1/page/render${uri}`, `/api/v1/page/json${uri}` — and
 * only `page_create` was normalizing first. The two that were not had a live correctness bug
 * and a latent security one:
 *
 *   - Live today: `page_verify({ path: '/a/../b' })` renders `/b` while the manifest reports
 *     `/a/../b`, and a `#` silently truncates the path. The tool's whole job is to report on
 *     the page it actually checked, so reporting a different path than it read is a
 *     correctness failure, not a cosmetic one.
 *   - Latent: `requestCore` policy-checks the RAW request string and normalizes afterwards,
 *     so a path like `/../../../../api/v1/users/current` would pass an `/api/v1/page/` prefix
 *     allowlist and then resolve somewhere else entirely. No `allow` policy is configured
 *     today, which is the only reason this is not already exploitable — and adding one is the
 *     natural next hardening step for this tool surface.
 */

/** The percent-decoded segments of a page path, with encoded separators refused. */
function decodeSegments(pathname: string, original: string, label: string): string[] {
    const segments = pathname
        .split('/')
        .filter(Boolean)
        .map((segment) => decodeURIComponent(segment));

    // Splitting first does not make an encoded slash inert: every caller rebuilds a path by
    // joining these segments, which turns it straight back into a path boundary. dotCMS
    // folder names cannot contain a separator anyway, so nothing legitimate is refused.
    const smuggled = segments.find((segment) => segment.includes('/'));
    if (smuggled !== undefined) {
        throw new Error(
            `${label} segment "${smuggled}" contains an encoded path separator (%2F), which ` +
                `would silently resolve to a DIFFERENT path than the one named: "${original}" ` +
                `would behave as if the slash had been written literally. dotCMS folder names ` +
                `cannot contain "/", so write the path out plainly instead.`
        );
    }

    return segments;
}

/** Run a path through the URL API, collapsing `.`/`..` and dropping any query or fragment. */
function toPathname(trimmed: string, original: string, label: string): string {
    // Collapse a leading `//` FIRST. To the URL API a `//` prefix is scheme-relative, so it
    // reads the next segment as a HOST: `new URL('//books/index', 'http://_').pathname` is
    // `/index`, silently discarding `books` — a page path resolving to a different page with
    // nothing to indicate it. (`///` does not even parse.) Callers reach this legitimately,
    // since `//host/path` is how dotCMS writes a host-qualified path elsewhere.
    const withoutSchemeRelative = trimmed.replace(/^\/+/, '/');

    try {
        // The base is a throwaway — only `pathname` is read back out, so the host never leaks
        // into the result.
        return new URL(withoutSchemeRelative, 'http://_').pathname;
    } catch {
        throw new Error(`${label} is not a valid path: "${original}"`);
    }
}

/**
 * The canonical form of a page path: leading slash, `.`/`..` collapsed, query and fragment
 * dropped, percent-encoding decoded, encoded separators refused.
 *
 * Returned so the CALLER can report what it actually requested. A tool that normalizes for
 * the request but echoes the raw input in its manifest is telling the model about a page it
 * did not look at.
 */
export function normalizePagePath(path: string, label = 'path'): string {
    const trimmed = path.trim();
    if (!trimmed) {
        throw new Error(`${label} must not be empty.`);
    }

    const withSlash = trimmed.startsWith('/') ? trimmed : `/${trimmed}`;
    const segments = decodeSegments(toPathname(withSlash, path, label), path, label);

    if (segments.length === 0) {
        return '/';
    }

    // Re-encode each segment so the result is a valid URL path again: decoding happened only
    // to inspect the segments, and a space or `#` left raw here would break the request URL
    // this feeds. `encodeURIComponent` escapes `/` too, which is exactly right — a slash
    // inside a single segment is the case refused above.
    return `/${segments.map((segment) => encodeURIComponent(segment)).join('/')}`;
}

/**
 * Split a page-relative URL into the parent folder and the leaf url stored on the page.
 *
 *   "/books/index" → { folder: "/books", url: "index", fullPath: "/books/index" }
 *   "/books"       → { folder: "/books", url: "index", fullPath: "/books/index" }
 *   "/about-us/"   → { folder: "/about-us", url: "index", fullPath: "/about-us/index" }
 *   "/"            → { folder: "/", url: "index", fullPath: "/index" }
 *
 * Shares its normalization with {@link normalizePagePath} but NOT its output: the folder-vs-leaf
 * decision is page-create's alone. The URL API preserves a trailing slash but does not know that
 * "/about-us/" means a folder index while "/about-us" means a leaf url, and dotCMS pages always
 * have a leaf url (commonly "index"), so a path with no explicit leaf gets one — matching how the
 * admin UI and the rest of the platform address a folder's default page.
 *
 * Segments are returned DECODED here, because they become folder and page names rather than
 * parts of a URL.
 */
export function splitUrlPath(urlPath: string): { folder: string; url: string; fullPath: string } {
    const trimmed = urlPath.trim();
    if (!trimmed.startsWith('/')) {
        throw new Error(`urlPath must start with "/": "${urlPath}"`);
    }

    const pathname = toPathname(trimmed, urlPath, 'urlPath');
    const segments = decodeSegments(pathname, urlPath, 'urlPath');

    // No segments → the site root; the page is the root index.
    if (segments.length === 0) {
        return { folder: '/', url: 'index', fullPath: '/index' };
    }

    // A trailing slash means the whole path IS the folder and the page is its index. Otherwise the
    // last segment is the leaf url and everything before it is the folder. (segments is non-empty
    // here — the length===0 case returned above.)
    if (!pathname.endsWith('/')) {
        const url = segments[segments.length - 1];
        const folderSegments = segments.slice(0, -1);
        const folder = folderSegments.length ? `/${folderSegments.join('/')}` : '/';
        const fullPath = `${folder === '/' ? '' : folder}/${url}`;

        return { folder, url, fullPath };
    }

    const folder = `/${segments.join('/')}`;

    return { folder, url: 'index', fullPath: `${folder}/index` };
}
