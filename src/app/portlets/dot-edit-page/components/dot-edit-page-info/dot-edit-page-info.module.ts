import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotEditPageInfoComponent } from './dot-edit-page-info.component';
import { ButtonModule } from 'primeng/primeng';
import { DotCopyButtonModule } from '@components/dot-copy-button/dot-copy-button.module';
import { DotApiLinkModule } from '@components/dot-api-link/dot-api-link.module';
import { LOCATION_TOKEN } from 'src/app/providers';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        ButtonModule,
        DotCopyButtonModule,
        DotApiLinkModule,
        DotPipesModule
    ],
    exports: [DotEditPageInfoComponent],
    declarations: [DotEditPageInfoComponent],
    providers: [{ provide: LOCATION_TOKEN, useValue: window.location }]
})
export class DotEditPageInfoModule {}
