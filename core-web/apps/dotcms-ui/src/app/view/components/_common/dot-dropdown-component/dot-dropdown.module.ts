import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';

import { DotDropdownComponent } from './dot-dropdown.component';

@NgModule({
    imports: [CommonModule, ButtonModule, UiDotIconButtonModule],
    declarations: [DotDropdownComponent],
    exports: [DotDropdownComponent]
})
export class DotDropdownModule {}
