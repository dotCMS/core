import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotSiteSelectorModule } from '@components/_common/dot-site-selector/dot-site-selector.module';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotThemesService, PaginatorService } from '@dotcms/data-access';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';

import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';

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
