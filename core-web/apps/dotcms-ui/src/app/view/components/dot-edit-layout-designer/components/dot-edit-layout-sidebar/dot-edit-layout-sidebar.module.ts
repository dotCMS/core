import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';

import { DotActionButtonModule } from '@components/_common/dot-action-button/dot-action-button.module';
import { DotContainerSelectorLayoutModule } from '@components/dot-container-selector-layout/dot-container-selector-layout.module';
import { DotPipesModule } from '@pipes/dot-pipes.module';

import { DotEditLayoutSidebarComponent } from './dot-edit-layout-sidebar.component';

import { DotSidebarPropertiesModule } from '../dot-sidebar-properties/dot-sidebar-properties.module';

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
    providers: []
})
export class DotEditLayoutSidebarModule {}
