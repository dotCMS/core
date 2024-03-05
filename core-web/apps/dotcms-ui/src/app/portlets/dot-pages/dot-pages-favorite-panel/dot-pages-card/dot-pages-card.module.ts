import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotPagesFavoritePageEmptySkeletonComponent } from '@dotcms/ui';

import { DotPagesCardComponent } from './dot-pages-card.component';

import { DotPipesModule } from '../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        CardModule,
        DotPagesFavoritePageEmptySkeletonComponent,
        ButtonModule,
        TooltipModule,
        DotPipesModule
    ],
    declarations: [DotPagesCardComponent],
    exports: [DotPagesCardComponent]
})
export class DotPagesCardModule {}
