import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

import { DotThemesService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotThemeSelectorComponent } from './dot-theme-selector.component';

import { DotPipesModule } from '../../../../pipes/dot-pipes.module';
import { DotSiteSelectorModule } from '../../../_common/dot-site-selector/dot-site-selector.module';
import { DotDialogModule } from '../../../dot-dialog/dot-dialog.module';

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
        DotPipesModule,
        DotMessagePipe
    ],
    exports: [DotThemeSelectorComponent],
    providers: [DotThemesService]
})
export class DotThemeSelectorModule {}
