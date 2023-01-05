import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DotAutofocusModule } from '@directives/dot-autofocus/dot-autofocus.module';
import { DotIconModule } from '@dotcms/ui';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { DataViewModule } from 'primeng/dataview';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';
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
        DotAutofocusModule,
        ...SEARCHABLE_NGFACES_MODULES,
        DotIconModule,
        UiDotIconButtonModule,
        DotMessagePipeModule
    ],
    providers: []
})
export class SearchableDropDownModule {}
