import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotUnlicensedPorletComponent } from './dot-unlicensed-porlet.component';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule, DotPipesModule],
    declarations: [DotUnlicensedPorletComponent],
    exports: [DotUnlicensedPorletComponent]
})
export class DotUnlicensedPorletModule {}
