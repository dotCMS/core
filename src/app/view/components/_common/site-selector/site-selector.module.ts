import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { SiteSelectorComponent } from './site-selector.component';

@NgModule({
    declarations: [SiteSelectorComponent],
    exports: [SiteSelectorComponent],
    imports: [CommonModule, FormsModule, SearchableDropDownModule]
})
export class SiteSelectorModule {}
