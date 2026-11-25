import { RenderMode, ServerRoute } from '@angular/ssr';

/**
 * Render mode per route. The first matching entry wins, so put specific paths
 * before the `**` catch-all.
 *
 *  - RenderMode.Server    -> HTML rendered on every request (SSR)
 *  - RenderMode.Client    -> SPA: the server returns the empty shell (index.csr.html)
 *  - RenderMode.Prerender -> HTML generated at build time
 *
 * Example: serve the blog as a SPA and everything else with SSR:
 *   { path: 'blog/**', renderMode: RenderMode.Client },
 */
export const serverRoutes: ServerRoute[] = [
  {
    path: '**',
    renderMode: RenderMode.Server
  }
];
