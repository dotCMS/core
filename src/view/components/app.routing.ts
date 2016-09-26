import {ModuleWithProviders}  from '@angular/core';
import {Routes, RouterModule} from '@angular/router';
import {ForgotPasswordContainer} from './common/login/forgot-password-component/forgot-password-container';
import {IframeLegacyComponent} from './common/iframe-legacy/Iframe-legacy-component';
import {LoginContainer} from './common/login/login-component/login-container';
import {LoginPageComponent} from './common/login/login-page-component';
import {MainComponent} from './common/main-component/main-component';
import {AppComponent} from './app';
import {ResetPasswordContainer} from './common/login/reset-password-component/reset-password-container';

const appRoutes: Routes = [
    {
        path: 'build',
        component: AppComponent
    },
    {
        path: 'dotCMS',
        component: MainComponent,
        children: [
            {
                path: '',
                redirectTo: 'portlet/EXT_21',
                pathMatch: 'full'

            },
            {
                component: IframeLegacyComponent,
                path: 'portlet/:id'

            }
        ]
    },
    {
        path: 'public',
        component: LoginPageComponent,
        children: [
            {
                path: 'forgotPassword',
                component: ForgotPasswordContainer
            },
            {
                path: 'login',
                component: LoginContainer
            },
            {
                path: 'resetPassword',
                component: ResetPasswordContainer
            }
        ]
    }
];

export const routing: ModuleWithProviders = RouterModule.forRoot(appRoutes);