import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotLinkModule } from '@components/dot-link/dot-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageInfoComponent } from './dot-edit-page-info.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        DotCopyButtonModule,
        DotLinkModule,
        DotApiLinkModule,
        DotPipesModule
    ],
    exports: [DotEditPageInfoComponent],
    declarations: [DotEditPageInfoComponent],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoModule {}
