import { Route } from '@angular/router';

import { DotEmaComponent } from './dot-ema-shell/dot-ema.component';

export const DotEmaRoutes: Route[] = [
    {
        path: '',
        component: DotEmaComponent,

        children: [
            {
                path: 'content',
                loadComponent: () =>
                    import('./edit-ema-editor/edit-ema-editor.component').then(
                        (mod) => mod.EditEmaEditorComponent
                    )
            },
            {
                path: 'layout',
                loadComponent: () =>
                    import('./edit-ema-layout/edit-ema-layout.component').then(
                        (mod) => mod.EditEmaLayoutComponent
                    )
            },
            {
                path: 'rules',
                loadComponent: () =>
                    import('./edit-ema-rules/edit-ema-rules.component').then(
                        (mod) => mod.EditEmaRulesComponent
                    )
            },
            {
                path: 'experiments',
                loadComponent: () =>
                    import('./edit-ema-experiments/edit-ema-experiments.component').then(
                        (mod) => mod.EditEmaExperimentsComponent
                    )
            },
            {
                path: '**',
                redirectTo: 'content',
                pathMatch: 'full'
            },
            {
                path: '',
                redirectTo: 'content',
                pathMatch: 'full'
            }
        ]
    }
];
