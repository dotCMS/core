import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import {
    ButtonModule,
    DataGridModule,
    DialogModule,
    DropdownModule,
    InputTextModule
} from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';
import { PaginatorService } from '@services/paginator';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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
        DataGridModule,
        DotDialogModule,
        DotIconModule,
        DotPipesModule
    ],
    exports: [DotThemeSelectorComponent],
    providers: [DotThemesService, PaginatorService]
})
export class DotThemeSelectorModule {}
