import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';

import { DotContentTypeSelectorComponent } from './dot-content-type-selector.component';

@NgModule({
    imports: [CommonModule, DropdownModule, FormsModule],
    declarations: [DotContentTypeSelectorComponent],
    exports: [DotContentTypeSelectorComponent]
})
export class DotContentTypeSelectorModule {}
