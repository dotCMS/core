import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { SharedModule } from 'primeng/api';
import { ButtonModule } from 'primeng/button';
import { TooltipModule } from 'primeng/tooltip';

import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotAddPersonaDialogModule } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.module';
import { DotPersonaSelectedItemModule } from '@components/dot-persona-selected-item/dot-persona-selected-item.module';
import { DotPersonaSelectorOptionModule } from '@components/dot-persona-selector-option/dot-persona-selector-option.module';
import { DotPersonalizeService, DotPersonasService, PaginatorService } from '@dotcms/data-access';
import { DotIconModule, DotMessagePipe, DotSafeHtmlPipe } from '@dotcms/ui';

import { DotPersonaSelectorComponent } from './dot-persona-selector.component';

@NgModule({
    declarations: [DotPersonaSelectorComponent],
    exports: [DotPersonaSelectorComponent],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule,
        DotPersonaSelectedItemModule,
        DotPersonaSelectorOptionModule,
        DotSafeHtmlPipe,
        DotIconModule,
        ButtonModule,
        TooltipModule,
        SharedModule,
        DotAddPersonaDialogModule,
        DotMessagePipe
    ],
    providers: [PaginatorService, DotPersonasService, DotPersonalizeService, IframeOverlayService]
})
export class DotPersonaSelectorModule {}
