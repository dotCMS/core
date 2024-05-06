import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { PaginatorService } from '@dotcms/data-access';
import { DotSafeHtmlPipe } from '@dotcms/ui';

import { DotContainerSelectorLayoutComponent } from './dot-container-selector-layout.component';

import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';

@NgModule({
    declarations: [DotContainerSelectorLayoutComponent],
    exports: [DotContainerSelectorLayoutComponent],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropDownModule,
        DotSafeHtmlPipe,
        DotContainerSelectorModule
    ],
    providers: [PaginatorService]
})
export class DotContainerSelectorLayoutModule {}
