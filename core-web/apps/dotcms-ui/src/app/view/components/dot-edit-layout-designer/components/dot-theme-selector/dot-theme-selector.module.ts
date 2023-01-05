import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotThemesService } from '@dotcms/data-access';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';

import { FormsModule } from '@angular/forms';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { DialogModule } from 'primeng/dialog';
import { DropdownModule } from 'primeng/dropdown';
import { InputTextModule } from 'primeng/inputtext';

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
        DotPipesModule
    ],
    exports: [DotThemeSelectorComponent],
    providers: [DotThemesService]
})
export class DotThemeSelectorModule {}
