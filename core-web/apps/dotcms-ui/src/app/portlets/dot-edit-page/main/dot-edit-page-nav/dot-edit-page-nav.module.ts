import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotPageToolsSeoComponent } from '@dotcms/portlets/dot-ema/ui';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotEditPageNavComponent } from './dot-edit-page-nav.component';

import { DotPipesModule } from '../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        RouterModule,
        TooltipModule,
        DotIconModule,
        DotPipesModule,
        DotMessagePipe,
        DotPageToolsSeoComponent
    ],
    declarations: [DotEditPageNavComponent],
    exports: [DotEditPageNavComponent]
})
export class DotEditPageNavModule {}
