import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

import { DotIconModule, DotMessagePipe } from '@dotcms/ui';

import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';

import { DotDeviceSelectorModule } from '../../../../../view/components/dot-device-selector/dot-device-selector.module';
import { DotLanguageSelectorComponent } from '../../../../../view/components/dot-language-selector/dot-language-selector.component';
import { DotPersonaSelectorModule } from '../../../../../view/components/dot-persona-selector/dot-persona.selector.module';
import { DotPipesModule } from '../../../../../view/pipes/dot-pipes.module';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorComponent,
        DotDeviceSelectorModule,
        DotPipesModule,
        DotIconModule,
        DotMessagePipe
    ],
    declarations: [DotEditPageViewAsControllerComponent],
    exports: [DotEditPageViewAsControllerComponent]
})
export class DotEditPageViewAsControllerModule {}
