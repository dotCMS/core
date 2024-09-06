import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';
import { ChipsModule } from 'primeng/chips';

import { DotTagsService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotAutocompleteTagsComponent } from './dot-autocomplete-tags.component';

@NgModule({
    imports: [
        CommonModule,
        ChipsModule,
        AutoCompleteModule,
        FormsModule,
        DotIconModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    declarations: [DotAutocompleteTagsComponent],
    providers: [DotTagsService],
    exports: [DotAutocompleteTagsComponent, ChipsModule]
})
export class DotAutocompleteTagsModule {}
