import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { OverlayPanelModule } from 'primeng/overlaypanel';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotFieldHelperComponent } from '@components/dot-field-helper/dot-field-helper.component';

@NgModule({
    imports: [CommonModule, UiDotIconButtonModule, OverlayPanelModule],
    declarations: [DotFieldHelperComponent],
    exports: [DotFieldHelperComponent]
})
export class DotFieldHelperModule {}
