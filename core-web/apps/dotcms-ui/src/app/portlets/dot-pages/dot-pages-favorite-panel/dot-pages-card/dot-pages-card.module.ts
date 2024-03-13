import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { CardModule } from 'primeng/card';
import { TooltipModule } from 'primeng/tooltip';

import { DotPagesFavoritePageEmptySkeletonComponent, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPagesCardComponent } from './dot-pages-card.component';

@NgModule({
    imports: [
        CommonModule,
        CardModule,
        DotPagesFavoritePageEmptySkeletonComponent,
        ButtonModule,
        TooltipModule,
        DotSafeHtmlPipe
    ],
    declarations: [DotPagesCardComponent],
    exports: [DotPagesCardComponent]
})
export class DotPagesCardModule {}
