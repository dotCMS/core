import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DotSiteSelectorComponent } from './dot-site-selector.component';

import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';

@NgModule({
    declarations: [DotSiteSelectorComponent],
    exports: [DotSiteSelectorComponent],
    imports: [CommonModule, FormsModule, SearchableDropDownModule]
})
export class DotSiteSelectorModule {}
