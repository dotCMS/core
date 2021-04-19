import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotUnlicensedPorletComponent } from './dot-unlicensed-porlet.component';
import { ButtonModule } from 'primeng/button';
import { DotIconModule } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [CommonModule, ButtonModule, DotIconModule, DotPipesModule],
    declarations: [DotUnlicensedPorletComponent],
    exports: [DotUnlicensedPorletComponent]
})
export class DotUnlicensedPorletModule {}
