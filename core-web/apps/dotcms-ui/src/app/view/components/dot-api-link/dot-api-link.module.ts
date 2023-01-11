import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotApiLinkComponent } from './dot-api-link.component';

@NgModule({
    imports: [CommonModule],
    declarations: [DotApiLinkComponent],
    exports: [DotApiLinkComponent]
})
export class DotApiLinkModule {}
