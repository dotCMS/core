import { SearchableDropdownComponent } from './component';
import { NgModule } from '@angular/core';
import { DataViewModule } from 'primeng/dataview';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DotIconModule } from '@dotcms/ui';
import { UiDotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';
import { DotMessagePipeModule } from '@pipes/dot-message/dot-message-pipe.module';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { OverlayPanelModule } from 'primeng/overlaypanel';

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
        ...SEARCHABLE_NGFACES_MODULES,
        DotIconModule,
        UiDotIconButtonModule,
        DotMessagePipeModule
    ],
    providers: []
})
export class SearchableDropDownModule {}
