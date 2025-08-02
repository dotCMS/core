import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

import { DotFieldHelperComponent } from './dot-field-helper.component';

@NgModule({
    imports: [CommonModule, ButtonModule, OverlayPanelModule],
    declarations: [DotFieldHelperComponent],
    exports: [DotFieldHelperComponent]
})
export class DotFieldHelperModule {}
