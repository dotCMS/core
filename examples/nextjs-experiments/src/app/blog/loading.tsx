/**
 * What the blog shows while it streams. With this file the blog is a streamed route: the page
 * answers at once with this fallback, and the blog, with its experiment's markup, arrives when
 * dotCMS answers. It shows an experiment on such a route: a returning visitor is sent to their
 * variant when that part arrives, and the SDK decides the marks as they stream in. The catch-all
 * route renders in one piece, for comparison.
 */
export default function Loading() {
    return <p className="p-8 text-center text-gray-500">Loading the blog…</p>;
}
