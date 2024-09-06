import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent
} from '@dotcms/ui';

import { ForgotPasswordComponent } from './forgot-password.component';

const routes: Routes = [
    {
        component: ForgotPasswordComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        InputTextModule,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        RouterModule.forChild(routes),
        DotFieldRequiredDirective
    ],
    declarations: [ForgotPasswordComponent]
})
export class ForgotPasswordModule {}
