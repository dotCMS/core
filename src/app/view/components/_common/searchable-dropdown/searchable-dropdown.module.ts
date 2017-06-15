
import { SearchableDropdownComponent } from './component';
import { NgModule } from '@angular/core';
import { OverlayPanelModule, ButtonModule, InputTextModule, PaginatorModule } from 'primeng/primeng';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

export const SEARCHABLE_NGFACES_MODULES = [
    ButtonModule,
    CommonModule,
    FormsModule,
    InputTextModule,
    OverlayPanelModule,
    PaginatorModule
];

@NgModule({
    declarations: [
        SearchableDropdownComponent
    ],
    exports: [
        SearchableDropdownComponent
    ],
    imports: [
        ...SEARCHABLE_NGFACES_MODULES
    ],
    providers: [
    ]
})
export class SearchableDropDownModule {

}
