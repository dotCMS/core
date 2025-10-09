import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotContainerSelectorComponent } from './dot-container-selector.component';

import { SearchableDropdownComponent } from '../_common/searchable-dropdown/component/searchable-dropdown.component';

@NgModule({
    declarations: [DotContainerSelectorComponent],
    exports: [DotContainerSelectorComponent],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropdownComponent,
        DotSafeHtmlPipe,
        DotMessagePipe
    ]
})
export class DotContainerSelectorModule {}
