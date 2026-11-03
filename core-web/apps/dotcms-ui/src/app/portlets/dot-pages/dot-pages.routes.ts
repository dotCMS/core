import { Routes } from '@angular/router';

import { DotPagesComponent } from './dot-pages.component';

/**
 * The Pages portlet. A page opens in the side panel over the listing and the portlet's URL names
 * it (`editContent`, `editContentLang`); the listing reads its URL on load (#38303, FR-049).
 *
 * `/pages/<inode>` and `/pages/new/<type>` are links main already had, and FR-026 keeps every
 * `/<portlet>/<inode>` link working: they redirect to `/pages` with the panel params, replacing the
 * path form in history, so they need no `reuseRoute` flag.
 */
const loadDeepLink = () =>
    import('../dot-porlet-detail/dot-edit-content-deep-link/dot-edit-content-deep-link.component').then(
        (m) => m.DotEditContentDeepLinkComponent
    );

export const dotPagesRoutes: Routes = [
    {
        component: DotPagesComponent,
        path: '',
        children: [
            { path: 'new/:contentType', loadComponent: loadDeepLink },
            { path: ':asset', loadComponent: loadDeepLink }
        ]
    }
];
