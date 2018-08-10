import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotThemeSelectorComponent } from './dot-theme-selector.component';
import { DotThemesService } from '../../../../../api/services/dot-themes/dot-themes.service';
import { ButtonModule, DataGridModule, DialogModule, DropdownModule, InputTextModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { SiteSelectorModule } from '../../../../../view/components/_common/site-selector/site-selector.module';
import { DotIconModule } from '../../../../../view/components/_common/dot-icon/dot-icon.module';

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
        DotIconModule
    ],
    exports: [DotThemeSelectorComponent],
    providers: [DotThemesService]
})
export class DotThemeSelectorModule {}
