import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { ButtonModule, InputTextModule } from 'primeng/primeng';

import { ForgotPasswordComponent} from './forgot-password.component';
import { ForgotPasswordContainerComponent } from './forgot-password-container.component';

const routes: Routes = [
    {
        component: ForgotPasswordContainerComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        InputTextModule,
        RouterModule.forChild(routes)
    ],
    declarations: [
        ForgotPasswordComponent,
        ForgotPasswordContainerComponent
    ]
})
export class ForgotPasswordModule { }
