import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputTextModule } from 'primeng/inputtext';

import { DotThemesService, PaginatorService } from '@dotcms/data-access';
import { DotIconComponent, DotMessagePipe } from '@dotcms/ui';

import { DotThemeSelectorDropdownComponent } from './dot-theme-selector-dropdown.component';

import { DotSiteSelectorComponent } from '../_common/dot-site-selector/dot-site-selector.component';
import { SearchableDropdownComponent } from '../_common/searchable-dropdown/component/searchable-dropdown.component';

@NgModule({
    declarations: [DotThemeSelectorDropdownComponent],
    exports: [DotThemeSelectorDropdownComponent],
    providers: [PaginatorService, DotThemesService],
    imports: [
        CommonModule,
        SearchableDropdownComponent,
        FormsModule,
        DotMessagePipe,
        DotSiteSelectorComponent,
        InputTextModule,
        DotIconComponent
    ]
})
export class DotThemeSelectorDropdownModule {}
