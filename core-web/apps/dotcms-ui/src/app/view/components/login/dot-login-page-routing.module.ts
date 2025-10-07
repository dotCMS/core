import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';

const routes: Routes = [
    {
        path: 'forgotPassword',
        loadChildren: () =>
            import('./forgot-password-component/forgot-password.module').then(
                (m) => m.ForgotPasswordModule
            )
    },
    {
        path: 'login',
        loadChildren: () =>
            import('./dot-login-component/dot-login.module').then((m) => m.DotLoginModule)
    },
    {
        path: 'resetPassword/:token',
        loadChildren: () =>
            import('./reset-password-component/reset-password.module').then(
                (m) => m.ResetPasswordModule
            )
    },
    {
        path: '',
        pathMatch: 'full',
        redirectTo: '/login'
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotLoginPageRoutingModule {}
