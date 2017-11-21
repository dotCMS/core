import { ContainerSelectorModule } from './../../view/components/container-selector/container-selector.module';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutGridComponent } from './layout/dot-edit-layout-grid/dot-edit-layout-grid.component';
import { DotEditLayoutComponent } from './layout/dot-edit-layout/dot-edit-layout.component';
import { NgGridModule } from 'angular2-grid';
import { IconButtonTooltipModule } from '../../view/components/_common/icon-button-tooltip/icon-button-tooltip.module';
import { ActionButtonModule } from '../../view/components/_common/action-button/action-button.module';
import { DotEditPageRoutingModule } from './dot-edit-page-routing.module';

@NgModule({
    imports: [
        CommonModule,
        NgGridModule,
        IconButtonTooltipModule,
        ActionButtonModule,
        DotEditPageRoutingModule,
        ContainerSelectorModule
    ],
    declarations: [
        DotEditLayoutGridComponent,
        DotEditLayoutComponent
    ]
})
export class DotEditPageModule {}
