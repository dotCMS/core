import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutComponent } from './dot-edit-layout.component';
import { DotEditLayoutGridModule } from '../dot-edit-layout-grid/dot-edit-layout-grid.module';
import { DotEditLayoutService } from '../../shared/services/dot-edit-layout.service';
import { CheckboxModule, ButtonModule, InputTextModule } from 'primeng/primeng';
import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';

@NgModule({
    declarations: [DotEditLayoutComponent],
    imports: [
        ButtonModule,
        CheckboxModule,
        CommonModule,
        DotActionButtonModule,
        DotEditLayoutGridModule,
        InputTextModule
    ],
    exports: [DotEditLayoutComponent],
    providers: [DotEditLayoutService]
})
export class DotEditLayoutModule {}
