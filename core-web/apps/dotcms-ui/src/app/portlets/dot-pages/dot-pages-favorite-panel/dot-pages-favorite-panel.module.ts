import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';
import { PanelModule } from 'primeng/panel';

import { DotMessagePipeModule } from '@dotcms/app/view/pipes/dot-message/dot-message-pipe.module';
import { DotIconModule } from '@dotcms/ui';

import { DotPagesCardModule } from './dot-pages-card/dot-pages-card.module';
import { DotPagesFavoritePanelComponent } from './dot-pages-favorite-panel.component';

@NgModule({
    imports: [
        CommonModule,
        DotIconModule,
        DotMessagePipeModule,
        DotPagesCardModule,
        PanelModule,
        ButtonModule
    ],
    declarations: [DotPagesFavoritePanelComponent],
    exports: [DotPagesFavoritePanelComponent]
})
export class DotPagesFavoritePanelModule {}
