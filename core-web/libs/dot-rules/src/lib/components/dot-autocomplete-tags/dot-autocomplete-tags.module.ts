import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ChipModule } from 'primeng/chip';

import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';

@NgModule({
    imports: [CommonModule, ChipModule, AutoCompleteModule, FormsModule],
    declarations: [DotAutocompleteTagsComponent],
    exports: [DotAutocompleteTagsComponent, ChipModule]
})
export class DotAutocompleteTagsModule {}
