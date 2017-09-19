import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { DotLoadingIndicatorModule  } from '../../_common/iframe/dot-loading-indicator/dot-loading-indicator.module';
import { ButtonModule, CheckboxModule, DropdownModule, InputTextModule } from 'primeng/primeng';

import { LoginContainerComponent } from './login-container.component';
import { LoginComponent } from './login.component';

import { SharedModule} from '../../../../shared/shared.module';

const routes: Routes = [
    {
        component: LoginContainerComponent,
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
        DotLoadingIndicatorModule
    ],
    declarations: [
        LoginContainerComponent,
        LoginComponent
    ]
})
export class LoginModule { }
