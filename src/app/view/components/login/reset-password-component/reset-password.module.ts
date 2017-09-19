import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';

import { ButtonModule, InputTextModule } from 'primeng/primeng';

import { ResetPasswordContainerComponent} from './reset-password-container.component';
import { ResetPasswordComponent} from './reset-password.component';

const routes: Routes = [
    {
        component: ResetPasswordContainerComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        InputTextModule,
        ButtonModule,
        RouterModule.forChild(routes)
    ],
    declarations: [
        ResetPasswordContainerComponent,
        ResetPasswordComponent
    ]
})
export class ResetPasswordModule { }
