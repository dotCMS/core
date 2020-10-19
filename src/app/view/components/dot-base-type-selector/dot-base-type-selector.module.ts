import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DropdownModule } from 'primeng/dropdown';
import { FormsModule } from '@angular/forms';
import { DotBaseTypeSelectorComponent } from './dot-base-type-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule],
    declarations: [DotBaseTypeSelectorComponent],
    exports: [DotBaseTypeSelectorComponent]
})
export class DotBaseTypeSelectorModule {}
