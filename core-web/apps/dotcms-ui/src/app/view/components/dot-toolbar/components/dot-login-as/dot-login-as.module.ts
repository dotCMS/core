import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { PasswordModule } from 'primeng/password';

import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotDialogModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotLoginAsComponent } from './dot-login-as.component';

@NgModule({
    imports: [
        CommonModule,
        DotDialogModule,
        PasswordModule,
        ReactiveFormsModule,
        SearchableDropDownModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    exports: [DotLoginAsComponent],
    declarations: [DotLoginAsComponent],
    providers: []
})
export class DotLoginAsModule {}
