import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { Chip } from 'primeng/chip';

import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';

@NgModule({
    imports: [CommonModule, Chip, AutoCompleteModule, FormsModule],
    declarations: [DotAutocompleteTagsComponent],
    exports: [DotAutocompleteTagsComponent]
})
export class DotAutocompleteTagsModule {}
