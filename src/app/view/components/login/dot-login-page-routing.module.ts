import { NgModule } from '@angular/core';
import { Routes, RouterModule } from '@angular/router';

const routes: Routes = [
    {
        path: 'forgotPassword',
        loadChildren:
            '@components/login/forgot-password-component/forgot-password.module#ForgotPasswordModule'
    },
    {
        path: 'login',
        loadChildren: '@components/login/dot-login-component/dot-login.module#DotLoginModule'
    },
    {
        path: 'resetPassword/:token',
        loadChildren:
            '@components/login/reset-password-component/reset-password.module#ResetPasswordModule'
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
