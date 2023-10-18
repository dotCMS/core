import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule, Routes } from '@angular/router';

import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { DotLoginComponent } from '@components/login/dot-login-component/dot-login.component';
import {
    DotAutofocusDirective,
    DotFieldRequiredDirective,
    DotFieldValidationMessageComponent
} from '@dotcms/ui';
import { DotDirectivesModule } from '@shared/dot-directives.module';
import { SharedModule } from '@shared/shared.module';

import { DotLoadingIndicatorModule } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.module';

const routes: Routes = [
    {
        component: DotLoginComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        RouterModule.forChild(routes),
        FormsModule,
        ButtonModule,
        CheckboxModule,
        DropdownModule,
        InputTextModule,
        SharedModule,
        DotLoadingIndicatorModule,
        DotDirectivesModule,
        ReactiveFormsModule,
        DotFieldValidationMessageComponent,
        DotAutofocusDirective,
        DotFieldRequiredDirective
    ],
    declarations: [DotLoginComponent]
})
export class DotLoginModule {}
