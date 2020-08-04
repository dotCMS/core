import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '@components/_common/searchable-dropdown';
import { DotPersonaSelectorComponent } from './dot-persona-selector.component';
import { DotPersonaSelectorOptionModule } from '@components/dot-persona-selector-option/dot-persona-selector-option.module';
import { ButtonModule, SharedModule, TooltipModule } from 'primeng/primeng';
import { DotIconModule } from '@components/_common/dot-icon/dot-icon.module';
import { DotAvatarModule } from '@components/_common/dot-avatar/dot-avatar.module';
import { DotPersonaSelectedItemModule } from '@components/dot-persona-selected-item/dot-persona-selected-item.module';
import { PaginatorService } from '@services/paginator';
import { DotPersonasService } from '@services/dot-personas/dot-personas.service';
import { DotPersonalizeService } from '@services/dot-personalize/dot-personalize.service';
import { DotAddPersonaDialogModule } from '@components/dot-add-persona-dialog/dot-add-persona-dialog.module';
import { IframeOverlayService } from '@components/_common/iframe/service/iframe-overlay.service';
import { DotPipesModule } from '@pipes/dot-pipes.module';

@NgModule({
    declarations: [DotPersonaSelectorComponent],
    exports: [DotPersonaSelectorComponent],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule,
        DotPersonaSelectedItemModule,
        DotPersonaSelectorOptionModule,
        DotPipesModule,
        DotIconModule,
        DotAvatarModule,
        ButtonModule,
        TooltipModule,
        SharedModule,
        DotAddPersonaDialogModule
    ],
    providers: [PaginatorService, DotPersonasService, DotPersonalizeService, IframeOverlayService]
})
export class DotPersonaSelectorModule {}
