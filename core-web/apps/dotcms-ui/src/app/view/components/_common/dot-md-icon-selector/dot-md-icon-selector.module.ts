import { CommonModule } from '@angular/common';
import { CUSTOM_ELEMENTS_SCHEMA, NgModule } from '@angular/core';

import { DotMdIconSelectorComponent } from './dot-md-icon-selector.component';

@NgModule({
    declarations: [DotMdIconSelectorComponent],
    exports: [DotMdIconSelectorComponent],
    imports: [CommonModule],
    schemas: [CUSTOM_ELEMENTS_SCHEMA]
})
export class DotMdIconSelectorModule {}
