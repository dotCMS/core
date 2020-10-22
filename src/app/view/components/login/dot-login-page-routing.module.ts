import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

const routes: Routes = [
    {
        path: 'forgotPassword',
        loadChildren: () =>
            import('@components/login/forgot-password-component/forgot-password.module').then(
                (m) => m.ForgotPasswordModule
            )
    },
    {
        path: 'login',
        loadChildren: () =>
            import('@components/login/dot-login-component/dot-login.module').then(
                (m) => m.DotLoginModule
            )
    },
    {
        path: 'resetPassword/:token',
        loadChildren: () =>
            import('@components/login/reset-password-component/reset-password.module').then(
                (m) => m.ResetPasswordModule
            )
    },
    {
        path: '',
        redirectTo: '/login'
    }
];

@NgModule({
    imports: [RouterModule.forChild(routes)],
    exports: [RouterModule]
})
export class DotLoginPageRoutingModule {}
