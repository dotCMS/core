import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { AutoCompleteModule } from 'primeng/autocomplete';

import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPageSelectorComponent } from './dot-page-selector.component';
import { DotPageSelectorService } from './service/dot-page-selector.service';

import { DotDirectivesModule } from '../../../../shared/dot-directives.module';
import { DotFieldHelperModule } from '../../dot-field-helper/dot-field-helper.module';

@NgModule({
    imports: [
        CommonModule,
        AutoCompleteModule,
        FormsModule,
        DotDirectivesModule,
        DotFieldHelperModule,
        DotSafeHtmlPipe,
        DotMessagePipe
    ],
    declarations: [DotPageSelectorComponent],
    providers: [DotPageSelectorService],
    exports: [DotPageSelectorComponent]
})
export class DotPageSelectorModule {}
