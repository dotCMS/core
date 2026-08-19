/**
 * Absolute URL of the Accessibility Studio page list — the screen the run screen
 * returns to. Kept in its own module (rather than beside the route table) because the
 * run component imports it and `a11y.routes.ts` imports the run component.
 *
 * It has to be ABSOLUTE. The run screen is the `**` route, since a page path is
 * multi-segment and can't be a single route param, and Angular's `..` drops one URL
 * segment rather than one route level — so a relative hop from
 * `/agents/a11y/about-us/index` lands on `/agents/a11y/about-us`, which matches `**`
 * again and leaves the user on the same screen.
 *
 * Mirrors the mount path: `agents` in `app.routes.ts` + the `a11y` agent id in
 * `agent-registry.ts`.
 */
export const A11Y_PAGE_LIST_ROUTE = '/agents/a11y';
