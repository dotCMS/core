import { SearchableDropdownComponent } from './component';
import { NgModule } from '@angular/core';
import { OverlayPanelModule, ButtonModule, InputTextModule } from 'primeng/primeng';
import { DataViewModule } from 'primeng/dataview';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DotIconModule } from '../dot-icon/dot-icon.module';
import { DotIconButtonModule } from '../dot-icon-button/dot-icon-button.module';

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
    imports: [CommonModule, FormsModule, ...SEARCHABLE_NGFACES_MODULES, DotIconModule, DotIconButtonModule],
    providers: []
})
export class SearchableDropDownModule {}
