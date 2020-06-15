import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';

import { NgModule } from '@angular/core';

import { DotLoginAsComponent } from './dot-login-as.component';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { PasswordModule } from 'primeng/primeng';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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
