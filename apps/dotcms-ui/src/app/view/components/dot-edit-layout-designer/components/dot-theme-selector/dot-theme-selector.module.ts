import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';

import { FormsModule } from '@angular/forms';
import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotIconModule } from '@dotcms/ui';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { PaginatorService } from '@services/paginator';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DropdownModule } from 'primeng/dropdown';
import { ButtonModule } from 'primeng/button';
import { DialogModule } from 'primeng/dialog';
import { InputTextModule } from 'primeng/inputtext';
import { DataViewModule } from 'primeng/dataview';

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
    providers: [DotThemesService, PaginatorService]
})
export class DotThemeSelectorModule {}
