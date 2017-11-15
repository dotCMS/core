import { ButtonModule } from 'primeng/primeng';
import { SearchableDropDownModule } from './../_common/searchable-dropdown/searchable-dropdown.module';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { ContainerSelectorComponent } from './container-selector.component';
import { NgModule } from '@angular/core';

@NgModule({
    declarations: [
        ContainerSelectorComponent,
    ],
    exports: [
        ContainerSelectorComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        ButtonModule,
        SearchableDropDownModule
    ]
})

export class ContainerSelectorModule {}
