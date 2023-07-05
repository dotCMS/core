import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotContainerSelectorComponent } from './dot-container-selector.component';

import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';

@NgModule({
    declarations: [DotContainerSelectorComponent],
    exports: [DotContainerSelectorComponent],
    imports: [CommonModule, FormsModule, ButtonModule, SearchableDropDownModule, DotPipesModule]
})
export class DotContainerSelectorModule {}
