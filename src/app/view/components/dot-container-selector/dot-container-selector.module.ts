import { ButtonModule } from 'primeng/primeng';
import { SearchableDropDownModule } from './../_common/searchable-dropdown/searchable-dropdown.module';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { DotContainerSelectorComponent } from './dot-container-selector.component';
import { NgModule } from '@angular/core';

@NgModule({
    declarations: [
        DotContainerSelectorComponent,
    ],
    exports: [
        DotContainerSelectorComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropDownModule
    ]
})

export class DotContainerSelectorModule {}
