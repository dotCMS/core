import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { TooltipModule } from 'primeng/tooltip';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageNavComponent } from './dot-edit-page-nav.component';

import { DotPageToolsSeoComponent } from '../../seo/components/dot-page-tools-seo/dot-page-tools-seo.component';

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
