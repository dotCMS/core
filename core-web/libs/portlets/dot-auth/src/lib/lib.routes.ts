import { Routes } from '@angular/router';

export const dotAuthRoutes: Routes = [
    {
        path: '',
        loadComponent: () =>
            import('./dot-auth-shell/dot-auth-shell.component').then((m) => m.DotAuthShellComponent)
    }
];
