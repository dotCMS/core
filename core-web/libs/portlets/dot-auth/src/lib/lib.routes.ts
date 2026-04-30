import { Routes } from '@angular/router';

export const dotAuthRoutes: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./dot-auth-shell/dot-auth-shell.component').then(
                (m) => m.DotAuthShellComponent
            ),
        children: [
            {
                path: '',
                loadComponent: () =>
                    import('./dot-auth-list/dot-auth-list.component').then(
                        (m) => m.DotAuthListComponent
                    )
            },
            {
                path: 'site/:hostId',
                loadComponent: () =>
                    import('./dot-auth-config/dot-auth-config.component').then(
                        (m) => m.DotAuthConfigComponent
                    )
            },
            {
                path: 'headless',
                loadComponent: () =>
                    import(
                        './dot-auth-headless-config/dot-auth-headless-config.component'
                    ).then((m) => m.DotAuthHeadlessConfigComponent)
            }
        ]
    }
];
