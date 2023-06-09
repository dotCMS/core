import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotIconModule, DotMessagePipeModule } from '@dotcms/ui';

import { SearchableDropdownComponent } from './component';

import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

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
        DotAutofocusModule,
        ...SEARCHABLE_NGFACES_MODULES,
        DotIconModule,
        UiDotIconButtonModule,
        DotMessagePipeModule
    ],
    providers: []
})
export class SearchableDropDownModule {}
