import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { InputSwitchModule } from 'primeng/inputswitch';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotEditPageStateControllerComponent } from './dot-edit-page-state-controller.component';

@NgModule({
    declarations: [DotEditPageStateControllerComponent, DotEditPageLockInfoComponent],
    exports: [DotEditPageStateControllerComponent],
    imports: [
        CommonModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        DotPipesModule,
        TooltipModule,
        DotMessagePipe
    ]
})
export class DotEditPageStateControllerModule {}
