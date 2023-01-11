import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { SplitButtonModule } from 'primeng/splitbutton';

import { DotPipesModule } from '@pipes/dot-pipes.module';

import { ActionHeaderComponent } from './action-header.component';

import { DotActionButtonModule } from '../../_common/dot-action-button/dot-action-button.module';


@NgModule({
    bootstrap: [],
    declarations: [ActionHeaderComponent],
    exports: [ActionHeaderComponent],
    imports: [CommonModule, DotActionButtonModule, SplitButtonModule, DotPipesModule],
    providers: []
})
export class ActionHeaderModule {}
