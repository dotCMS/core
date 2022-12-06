import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { CardModule } from 'primeng/card';
import { DotPagesCardComponent } from './dot-pages-card.component';
import { UiDotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { TooltipModule } from 'primeng/tooltip';

@NgModule({
    imports: [CommonModule, CardModule, UiDotIconButtonModule, TooltipModule, DotPipesModule],
    declarations: [DotPagesCardComponent],
    exports: [DotPagesCardComponent]
})
export class DotPagesCardModule {}
