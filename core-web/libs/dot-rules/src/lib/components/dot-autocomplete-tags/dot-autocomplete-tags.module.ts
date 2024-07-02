import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ChipsModule } from 'primeng/chips';

import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';

@NgModule({
    imports: [CommonModule, ChipsModule, AutoCompleteModule, FormsModule],
    declarations: [DotAutocompleteTagsComponent],
    exports: [DotAutocompleteTagsComponent, ChipsModule]
})
export class DotAutocompleteTagsModule {}
