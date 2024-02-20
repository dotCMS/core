import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotAutofocusDirective, DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { SearchableDropdownComponent } from './component';

export const SEARCHABLE_NGFACES_MODULES = [
    ButtonModule,
    CommonModule,
    DataViewModule,
    FormsModule,
    InputTextModule,
    OverlayPanelModule
];

@NgModule({
    declarations: [SearchableDropdownComponent],
    exports: [SearchableDropdownComponent],
    imports: [
        CommonModule,
        FormsModule,
        DotAutofocusDirective,
        ...SEARCHABLE_NGFACES_MODULES,
        DotIconModule,
        DotMessagePipe
    ],
    providers: []
})
export class SearchableDropDownModule {}
