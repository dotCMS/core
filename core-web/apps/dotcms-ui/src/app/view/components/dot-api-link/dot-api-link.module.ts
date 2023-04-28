import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotLinkModule } from '@components/dot-link/dot-link.module';
import { DotIconModule } from '@dotcms/ui';

import { DotApiLinkComponent } from './dot-api-link.component';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule, DotLinkModule],
    declarations: [DotApiLinkComponent],
    exports: [DotApiLinkComponent]
})
export class DotApiLinkModule {}
