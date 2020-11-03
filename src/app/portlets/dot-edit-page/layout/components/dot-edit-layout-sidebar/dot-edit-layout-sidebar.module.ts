import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotEditLayoutSidebarComponent } from './dot-edit-layout-sidebar.component';
import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorModule } from '@components/dot-container-selector/dot-container-selector.module';
import { ButtonModule } from 'primeng/button';
import { DotSidebarPropertiesModule } from '../dot-sidebar-properties/dot-sidebar-properties.module';
import { FormsModule } from '@angular/forms';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { DotEditLayoutService } from '@portlets/dot-edit-page/shared/services/dot-edit-layout.service';
import { TemplateContainersCacheService } from '@portlets/dot-edit-page/template-containers-cache.service';

@NgModule({
    declarations: [DotEditLayoutSidebarComponent],
    imports: [
        CommonModule,
        DotActionButtonModule,
        FormsModule,
        DotContainerSelectorModule,
        ButtonModule,
        DotSidebarPropertiesModule,
        DotPipesModule
    ],
    exports: [DotEditLayoutSidebarComponent],
    providers: [DotEditLayoutService, TemplateContainersCacheService]
})
export class DotEditLayoutSidebarModule {}
