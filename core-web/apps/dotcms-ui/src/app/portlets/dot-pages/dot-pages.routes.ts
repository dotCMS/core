import { Routes } from '@angular/router';

import { DotPagesComponent } from './dot-pages.component';

export const dotPagesRoutes: Routes = [
    {
        component: DotPagesComponent,
        path: '',
        children: [
            // Deep links to a page or a create open the side panel over the listing, which
            // picks the editor per content type (#38303).
            {
                loadComponent: () =>
                    import('../dot-porlet-detail/dot-edit-content-deep-link/dot-edit-content-deep-link.component').then(
                        (m) => m.DotEditContentDeepLinkComponent
                    ),
                path: ':asset',
                data: {
                    reuseRoute: false
                }
            },
            {
                loadComponent: () =>
                    import('../dot-porlet-detail/dot-edit-content-deep-link/dot-edit-content-deep-link.component').then(
                        (m) => m.DotEditContentDeepLinkComponent
                    ),
                path: 'new/:contentType'
            }
        ]
    }
];
