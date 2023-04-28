import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';

import { DotLinkComponent } from '././dot-link.component';

import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule, DotPipesModule],
    declarations: [DotLinkComponent],
    exports: [DotLinkComponent]
})
export class DotLinkModule {}
