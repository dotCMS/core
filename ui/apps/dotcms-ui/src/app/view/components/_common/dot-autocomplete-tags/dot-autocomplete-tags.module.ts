import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';
import { FormsModule } from '@angular/forms';
import { DotTagsService } from '@services/dot-tags/dot-tags.service';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ChipsModule } from 'primeng/chips';
import { AutoCompleteModule } from 'primeng/autocomplete';

@NgModule({
    imports: [
        CommonModule,
        ChipsModule,
        AutoCompleteModule,
        FormsModule,
        DotIconModule,
        DotPipesModule
    ],
    declarations: [DotAutocompleteTagsComponent],
    providers: [DotTagsService],
    exports: [DotAutocompleteTagsComponent, ChipsModule]
})
export class DotAutocompleteTagsModule {}
