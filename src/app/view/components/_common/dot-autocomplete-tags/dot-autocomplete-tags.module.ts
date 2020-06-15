import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AutoCompleteModule, ChipsModule } from 'primeng/primeng';
import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';
import { FormsModule } from '@angular/forms';
import { DotTagsService } from '@services/dot-tags/dot-tags.service';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

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
