import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotDropdownComponent } from './dot-dropdown.component';

@NgModule({
    imports: [CommonModule, ButtonModule],
    declarations: [DotDropdownComponent],
    exports: [DotDropdownComponent]
})
export class DotDropdownModule {}
