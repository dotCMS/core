import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotLinkComponent } from '@components/dot-link/dot-link.component';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotApiLinkComponent, DotCopyButtonComponent, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';

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
