import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AutoCompleteModule, ChipsModule } from 'primeng/primeng';
import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';
import { FormsModule } from '@angular/forms';
import { DotTagsService } from '@services/dot-tags/dot-tags.service';

@NgModule({
    imports: [CommonModule, ChipsModule, AutoCompleteModule, FormsModule],
    declarations: [DotAutocompleteTagsComponent],
    providers: [DotTagsService],
    exports: [DotAutocompleteTagsComponent, ChipsModule]
})
export class DotAutocompleteTagsModule {}
