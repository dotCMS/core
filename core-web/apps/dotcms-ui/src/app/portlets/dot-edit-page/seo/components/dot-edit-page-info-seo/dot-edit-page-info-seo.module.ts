import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { ButtonModule } from 'primeng/button';

import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotLinkComponent } from '@components/dot-link/dot-link.component';
import { LOCATION_TOKEN } from '@dotcms/app/providers';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageInfoSeoComponent } from './dot-edit-page-info-seo.component';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        DotCopyButtonModule,
        DotApiLinkModule,
        DotPipesModule,
        DotLinkComponent
    ],
    exports: [DotEditPageInfoSeoComponent],
    declarations: [DotEditPageInfoSeoComponent],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoSeoModule {}
