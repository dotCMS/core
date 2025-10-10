import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { DropdownModule } from 'primeng/dropdown';
import { TooltipModule } from 'primeng/tooltip';

import { DotIconComponent, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotEditPageViewAsControllerComponent } from './dot-edit-page-view-as-controller.component';

import { DotDeviceSelectorModule } from '../../../../../view/components/dot-device-selector/dot-device-selector.module';
import { DotLanguageSelectorComponent } from '../../../../../view/components/dot-language-selector/dot-language-selector.component';
import { DotPersonaSelectorComponent } from '../../../../../view/components/dot-persona-selector/dot-persona-selector.component';

@NgModule({
    imports: [
        CommonModule,
        DropdownModule,
        FormsModule,
        TooltipModule,
        DotPersonaSelectorComponent,
        DotLanguageSelectorComponent,
        DotDeviceSelectorModule,
        DotSafeHtmlPipe,
        DotIconComponent,
        DotMessagePipe
    ],
    declarations: [DotEditPageViewAsControllerComponent],
    exports: [DotEditPageViewAsControllerComponent]
})
export class DotEditPageViewAsControllerModule {}
