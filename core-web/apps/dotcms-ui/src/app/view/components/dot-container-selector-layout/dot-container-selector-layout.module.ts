import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { PaginatorService } from '@dotcms/data-access';

import { DotContainerSelectorLayoutComponent } from './dot-container-selector-layout.component';

import { DotPipesModule } from '../../pipes/dot-pipes.module';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { DotContainerSelectorModule } from '../dot-container-selector/dot-container-selector.module';

@NgModule({
    declarations: [DotContainerSelectorLayoutComponent],
    exports: [DotContainerSelectorLayoutComponent],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropDownModule,
        DotPipesModule,
        DotContainerSelectorModule
    ],
    providers: [PaginatorService]
})
export class DotContainerSelectorLayoutModule {}
