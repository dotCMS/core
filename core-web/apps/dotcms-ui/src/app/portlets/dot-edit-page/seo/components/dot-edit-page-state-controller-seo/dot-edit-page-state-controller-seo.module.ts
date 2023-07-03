import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { InputSwitchModule } from 'primeng/inputswitch';
import { SelectButtonModule } from 'primeng/selectbutton';
import { TooltipModule } from 'primeng/tooltip';

import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageLockInfoSeoComponent } from './components/dot-edit-page-lock-info/dot-edit-page-lock-info-seo.component';
import { DotEditPageStateControllerSeoComponent } from './dot-edit-page-state-controller-seo.component';

import { DotDeviceSelectorSeoModule } from '../dot-device-selector-seo/dot-device-selector-seo.module';

@NgModule({
    declarations: [DotEditPageStateControllerSeoComponent, DotEditPageLockInfoSeoComponent],
    exports: [DotEditPageStateControllerSeoComponent],
    imports: [
        CommonModule,
        FormsModule,
        InputSwitchModule,
        SelectButtonModule,
        DotPipesModule,
        TooltipModule,
        ButtonModule,
        DotDeviceSelectorSeoModule
    ]
})
export class DotEditPageStateControllerSeoModule {}
