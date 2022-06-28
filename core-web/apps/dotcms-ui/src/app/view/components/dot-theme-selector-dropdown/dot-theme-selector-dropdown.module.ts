import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';
import { PaginatorService } from '@services/paginator';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { FormsModule } from '@angular/forms';
import { DotThemesService } from '@services/dot-themes/dot-themes.service';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { InputTextModule } from 'primeng/inputtext';
import { DotIconModule } from '@dotcms/ui';

@NgModule({
    declarations: [DotThemeSelectorDropdownComponent],
    exports: [DotThemeSelectorDropdownComponent],
    providers: [PaginatorService, DotThemesService],
    imports: [
        CommonModule,
        SearchableDropDownModule,
        FormsModule,
        DotMessagePipeModule,
        DotSiteSelectorModule,
        InputTextModule,
        DotIconModule
    ]
})
export class DotThemeSelectorDropdownModule {}
