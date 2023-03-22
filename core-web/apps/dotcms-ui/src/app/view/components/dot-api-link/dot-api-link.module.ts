import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';

import { DotApiLinkComponent } from './dot-api-link.component';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule],
    declarations: [DotApiLinkComponent],
    exports: [DotApiLinkComponent]
})
export class DotApiLinkModule {}
