import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotBaseTypeSelectorComponent } from './dot-base-type-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule],
    declarations: [DotBaseTypeSelectorComponent],
    exports: [DotBaseTypeSelectorComponent]
})
export class DotBaseTypeSelectorModule {}
