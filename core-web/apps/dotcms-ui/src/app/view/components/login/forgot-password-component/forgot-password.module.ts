import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';

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
        DotFieldValidationMessageModule,
        DotAutofocusModule,
        RouterModule.forChild(routes)
    ],
    declarations: [ForgotPasswordComponent]
})
export class ForgotPasswordModule {}
