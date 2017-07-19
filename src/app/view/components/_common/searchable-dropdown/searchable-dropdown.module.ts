import { SearchableDropdownComponent } from './component';
import { NgModule } from '@angular/core';
import { OverlayPanelModule, ButtonModule, InputTextModule, DataListModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

export const SEARCHABLE_NGFACES_MODULES = [
    ButtonModule,
    CommonModule,
    DataListModule,
    FormsModule,
    InputTextModule,
    OverlayPanelModule
];

@NgModule({
    declarations: [
        SearchableDropdownComponent
    ],
    exports: [
        SearchableDropdownComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        ...SEARCHABLE_NGFACES_MODULES
    ],
    providers: []
})
export class SearchableDropDownModule {

}
