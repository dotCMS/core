import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ChipsModule } from 'primeng/chips';

@NgModule({
    imports: [CommonModule, ChipsModule, AutoCompleteModule, FormsModule],
    declarations: [DotAutocompleteTagsComponent],
    exports: [DotAutocompleteTagsComponent, ChipsModule]
})
export class DotAutocompleteTagsModule {}
