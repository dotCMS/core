import { Routes } from '@angular/router';

export const dotLoginPageRoutes: Routes = [
    {
        path: 'forgotPassword',
        loadComponent: () =>
            import('./forgot-password-component/forgot-password.component').then(
                (m) => m.ForgotPasswordComponent
            )
    },
    {
        path: 'login',
        loadComponent: () =>
            import('./dot-login-component/dot-login.component').then((m) => m.DotLoginComponent)
    },
    {
        path: 'resetPassword/:token',
        loadComponent: () =>
            import('./reset-password-component/reset-password.component').then(
                (m) => m.ResetPasswordComponent
            )
    },
    {
        path: '',
        pathMatch: 'full',
        redirectTo: '/login'
    }
];
