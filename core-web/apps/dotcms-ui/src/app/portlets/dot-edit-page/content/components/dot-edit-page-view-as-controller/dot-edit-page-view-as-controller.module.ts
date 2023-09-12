import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

import { DotDeviceSelectorModule } from '@components/dot-device-selector/dot-device-selector.module';
import { DotLanguageSelectorModule } from '@components/dot-language-selector/dot-language-selector.module';
import { DotPersonaSelectorModule } from '@components/dot-persona-selector/dot-persona.selector.module';
import { DotIconModule, DotMessagePipe } from '@dotcms/ui';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorModule,
        DotDeviceSelectorModule,
        DotPipesModule,
        DotIconModule,
        DotMessagePipe
    ],
    declarations: [DotEditPageViewAsControllerComponent],
    exports: [DotEditPageViewAsControllerComponent]
})
export class DotEditPageViewAsControllerModule {}
