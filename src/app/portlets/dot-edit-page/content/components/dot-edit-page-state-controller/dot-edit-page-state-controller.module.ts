import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { InputSwitchModule, SelectButtonModule } from 'primeng/primeng';

import { DotEditPageStateControllerComponent } from './dot-edit-page-state-controller.component';
import { DotEditPageLockInfoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    declarations: [DotEditPageStateControllerComponent, DotEditPageLockInfoComponent],
    exports: [DotEditPageStateControllerComponent],
    imports: [CommonModule, FormsModule, InputSwitchModule, SelectButtonModule, DotPipesModule]
})
export class DotEditPageStateControllerModule {}
