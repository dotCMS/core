# 02 · next.config

Four settings a dotCMS front end needs. Each exists for a reason that is not
obvious from the Next.js docs.

```ts
const url = new URL(dotCMSHost);

const nextConfig: NextConfig = {
  // UVE's bridge breaks under StrictMode's double-invoked effects.
  reactStrictMode: false,

  images: {
    remotePatterns: [{
      protocol: url.protocol.replace(':', '') as 'http' | 'https',
      hostname: url.hostname,
      port: url.port || '',
    }],
    loader: 'custom',
    loaderFile: './src/utils/imageLoader.ts',
  },

  // Lets relative /dA/ URLs inside content resolve without CORS.
  async rewrites() {
    return [{ source: '/dA/:path*', destination: `${dotCMSHost}/dA/:path*` }];
  },

  // dotCMS pages live at /path/index; collapse so one URL serves each page.
  async redirects() {
    return [{ source: '/:path*/index', destination: '/:path*/', permanent: true }];
  },
};
```

## The image loader

`loader: 'custom'` hands resizing to dotCMS instead of Next's optimizer. The loader
normalises any value to the `/dA/` delivery route and appends the width:

```ts
// Absolute URLs (external/stock imagery) are served as-is.
if (/^https?:\/\//.test(src)) return src;

const imageSRC = src.includes('/dA/') ? src : `/dA/${src}`;
return `${new URL(dotCMSHost).origin}${imageSRC}/${width}w`;
```

Two consequences worth knowing:

- **You pass an identifier, not a URL.** `<Image src={contentlet.image.identifier} />`
  works because the loader builds the path. An absolute `http(s)://` src short-circuits
  the loader and is returned untouched — but `remotePatterns` below only allows the
  dotCMS host, so an external image still fails Next's host check unless you add its
  host there. For one-off external imagery a plain `<img>` avoids the whole question.
- **`{width}w` is the resize syntax.** dotCMS ignores a bare `/dA/{id}/1000/80`;
  width and quality need their `w`/`q` suffixes. Getting this wrong returns the
  full-size original — or, for some WebP sources, a corrupt tiny image at HTTP 200.

`remotePatterns` is still required: Next validates remote hosts independently of the
loader.
