import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { TableModule } from 'primeng/table';

import { DotDialogComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotFormSelectorComponent } from './dot-form-selector.component';

@NgModule({
    imports: [
        CommonModule,
        TableModule,
        DotDialogComponent,
        ButtonModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    declarations: [DotFormSelectorComponent],
    exports: [DotFormSelectorComponent]
})
export class DotFormSelectorModule {}
