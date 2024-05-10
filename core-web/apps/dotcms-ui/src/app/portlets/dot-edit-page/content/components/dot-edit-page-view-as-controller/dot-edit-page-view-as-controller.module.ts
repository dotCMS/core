import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

import { DotDeviceSelectorModule } from '@components/dot-device-selector/dot-device-selector.module';
import { DotLanguageSelectorComponent } from '@components/dot-language-selector/dot-language-selector.component';
import { DotPersonaSelectorModule } from '@components/dot-persona-selector/dot-persona.selector.module';
import { DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorModule,
        DotLanguageSelectorComponent,
        DotDeviceSelectorModule,
        DotSafeHtmlPipe,
        DotIconModule,
        DotMessagePipe
    ],
    declarations: [DotEditPageViewAsControllerComponent],
    exports: [DotEditPageViewAsControllerComponent]
})
export class DotEditPageViewAsControllerModule {}
