import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { PasswordModule } from 'primeng/password';

import { DotMessagePipe } from '@dotcms/ui';

import { DotLoginAsComponent } from './dot-login-as.component';

import { DotPipesModule } from '../../../../pipes/dot-pipes.module';
import { SearchableDropDownModule } from '../../../_common/searchable-dropdown/searchable-dropdown.module';
import { DotDialogModule } from '../../../dot-dialog/dot-dialog.module';

@NgModule({
    imports: [
        CommonModule,
        DotDialogModule,
        PasswordModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        DotPipesModule,
        DotMessagePipe
    ],
    exports: [DotLoginAsComponent],
    declarations: [DotLoginAsComponent],
    providers: []
})
export class DotLoginAsModule {}
