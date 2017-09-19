import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Routes } from '@angular/router';

import { NotLicensedComponent } from './not-licensed.component';

const routes: Routes = [
    {
        component: NotLicensedComponent,
        path: ''
    }
];

@NgModule({
    imports: [
        CommonModule,
        RouterModule.forChild(routes)
    ],
    declarations: [
        NotLicensedComponent
    ]
})

export class NotLicensedModule { }
