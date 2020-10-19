import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ForgotPasswordComponent } from './forgot-password.component';
import { MdInputTextModule } from '@directives/md-inputtext/md-input-text.module';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';

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
        MdInputTextModule,
        ReactiveFormsModule,
        DotFieldValidationMessageModule,
        DotAutofocusModule,
        RouterModule.forChild(routes)
    ],
    declarations: [ForgotPasswordComponent]
})
export class ForgotPasswordModule {}
