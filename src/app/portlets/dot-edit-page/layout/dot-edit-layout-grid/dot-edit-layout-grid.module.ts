import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutGridComponent } from './dot-edit-layout-grid.component';
import { NgGridModule } from 'angular2-grid';
import { DotActionButtonModule } from '../../../../view/components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorModule } from '../../../../view/components/dot-container-selector/dot-container-selector.module';

@NgModule({
    declarations: [DotEditLayoutGridComponent],
    imports: [CommonModule, NgGridModule, DotActionButtonModule, DotContainerSelectorModule],
    exports: [DotEditLayoutGridComponent],
    providers: []
})
export class DotEditLayoutGridModule {}
