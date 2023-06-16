import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotPagesCardComponent } from './dot-pages-card.component';

import { DotPagesFavoritePageEmptySkeletonComponent } from '../../dot-pages-favorite-page-empty-skeleton/dot-pages-favorite-page-empty-skeleton.component';

@NgModule({
    imports: [
        CommonModule,
        CardModule,
        DotPagesFavoritePageEmptySkeletonComponent,
        UiDotIconButtonModule,
        TooltipModule,
        DotPipesModule
    ],
    declarations: [DotPagesCardComponent],
    exports: [DotPagesCardComponent]
})
export class DotPagesCardModule {}
