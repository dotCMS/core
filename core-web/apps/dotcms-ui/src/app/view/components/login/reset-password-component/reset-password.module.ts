import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { ResetPasswordComponent } from './reset-password.component';
import { DotFieldValidationMessageModule } from '@components/_common/dot-field-validation-message/dot-file-validation-message.module';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { InputTextModule } from 'primeng/inputtext';
import { ButtonModule } from 'primeng/button';

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
        DotFieldValidationMessageModule,
        DotAutofocusModule,
        RouterModule.forChild(routes)
    ],
    declarations: [ResetPasswordComponent]
})
export class ResetPasswordModule {}
