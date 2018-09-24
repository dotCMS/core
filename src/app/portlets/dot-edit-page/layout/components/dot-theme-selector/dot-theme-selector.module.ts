import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { ButtonModule, DataGridModule, DialogModule, DropdownModule, InputTextModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { SiteSelectorModule } from '@components/_common/site-selector/site-selector.module';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotDialogModule } from '@components/dot-dialog/dot-dialog.module';

@NgModule({
    declarations: [DotThemeSelectorComponent],
    imports: [
        CommonModule,
        DropdownModule,
        ButtonModule,
        FormsModule,
        DialogModule,
        SiteSelectorModule,
        InputTextModule,
        DataGridModule,
        DotDialogModule,
        DotIconModule
    ],
    exports: [DotThemeSelectorComponent],
    providers: [DotThemesService]
})
export class DotThemeSelectorModule {}
