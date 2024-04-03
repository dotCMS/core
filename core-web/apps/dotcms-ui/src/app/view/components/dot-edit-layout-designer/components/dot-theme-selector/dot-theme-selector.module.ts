import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotThemesService } from '@dotcms/data-access';
import { DotDialogModule, DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotThemeSelectorComponent } from './dot-theme-selector.component';

@NgModule({
    declarations: [DotThemeSelectorComponent],
    imports: [
        CommonModule,
        DropdownModule,
        ButtonModule,
        FormsModule,
        DialogModule,
        DotSiteSelectorModule,
        InputTextModule,
        DataViewModule,
        DotDialogModule,
        DotIconModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    exports: [DotThemeSelectorComponent],
    providers: [DotThemesService]
})
export class DotThemeSelectorModule {}
