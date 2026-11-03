import { Routes } from '@angular/router';

import { DotPagesComponent } from './dot-pages.component';

/**
 * The Pages portlet. A page opens in the side panel over the listing and the portlet's URL names
 * it (`editContent`, `editContentLang`), so there are no deep-link children: the listing reads its
 * URL on load (#38303, FR-049).
 */
export const dotPagesRoutes: Routes = [
    {
        component: DotPagesComponent,
        path: ''
    }
];
