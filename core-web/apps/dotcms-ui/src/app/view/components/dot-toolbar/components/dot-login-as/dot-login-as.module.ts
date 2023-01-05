import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

import { NgModule } from '@angular/core';

import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { PasswordModule } from 'primeng/password';
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
