import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotApiLinkComponent, DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';

import { LOCATION_TOKEN } from '../../../../providers';
import { DotLinkComponent } from '../../../../view/components/dot-link/dot-link.component';
import { DotPipesModule } from '../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        DotCopyButtonComponent,
        DotApiLinkComponent,
        DotPipesModule,
        DotLinkComponent,
        DotMessagePipe
    ],
    exports: [DotEditPageInfoComponent],
    declarations: [DotEditPageInfoComponent],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoModule {}
