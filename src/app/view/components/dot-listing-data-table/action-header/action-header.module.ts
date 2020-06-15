import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotActionButtonModule } from '../../_common/dot-action-button/dot-action-button.module';
import { ActionHeaderComponent } from './action-header.component';
import { SplitButtonModule } from 'primeng/primeng';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    bootstrap: [],
    declarations: [ActionHeaderComponent],
    exports: [ActionHeaderComponent],
    imports: [CommonModule, DotActionButtonModule, SplitButtonModule, DotPipesModule],
    providers: []
})
export class ActionHeaderModule {}
