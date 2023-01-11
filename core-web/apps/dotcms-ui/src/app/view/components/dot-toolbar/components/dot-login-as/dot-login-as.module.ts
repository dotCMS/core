import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { PasswordModule } from 'primeng/password';

import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotLoginAsComponent } from './dot-login-as.component';

@NgModule({
    imports: [
        CommonModule,
        DotDialogModule,
        PasswordModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        DotPipesModule
    ],
    exports: [DotLoginAsComponent],
    declarations: [DotLoginAsComponent],
    providers: []
})
export class DotLoginAsModule {}
