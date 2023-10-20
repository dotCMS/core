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

import { ResetPasswordComponent } from './reset-password.component';

const routes: Routes = [
    {
        component: ResetPasswordComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        FormsModule,
        InputTextModule,
        ButtonModule,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        RouterModule.forChild(routes),
        DotFieldRequiredDirective
    ],
    declarations: [ResetPasswordComponent]
})
export class ResetPasswordModule {}
