import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutSidebarComponent } from './dot-edit-layout-sidebar.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorLayoutModule } from '@components/dot-container-selector-layout/dot-container-selector-layout.module';
import { ButtonModule } from 'primeng/button';
import { DotSidebarPropertiesModule } from '../dot-sidebar-properties/dot-sidebar-properties.module';
import { FormsModule } from '@angular/forms';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotEditLayoutService } from '@services/dot-edit-layout/dot-edit-layout.service';
import { DotTemplateContainersCacheService } from '@services/dot-template-containers-cache/dot-template-containers-cache.service';

@NgModule({
    declarations: [DotEditLayoutSidebarComponent],
    imports: [
        CommonModule,
        DotActionButtonModule,
        FormsModule,
        DotContainerSelectorLayoutModule,
        ButtonModule,
        DotSidebarPropertiesModule,
        DotPipesModule
    ],
    exports: [DotEditLayoutSidebarComponent],
    providers: [DotEditLayoutService, DotTemplateContainersCacheService]
})
export class DotEditLayoutSidebarModule {}
