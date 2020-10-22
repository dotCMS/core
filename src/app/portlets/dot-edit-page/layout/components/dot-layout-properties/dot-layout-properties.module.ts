import { DotIconButtonModule } from '@components/_common/dot-icon-button/dot-icon-button.module';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { ReactiveFormsModule } from '@angular/forms';

import { DotLayoutSidebarModule } from './dot-layout-property-sidebar/dot-layout-property-sidebar.module';
import { DotLayoutPropertiesItemModule } from './dot-layout-properties-item/dot-layout-properties-item.module';
import { DotLayoutPropertiesComponent } from './dot-layout-properties.component';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';

@NgModule({
    declarations: [DotLayoutPropertiesComponent],
    imports: [
        CommonModule,
        DotLayoutPropertiesItemModule,
        DotLayoutSidebarModule,
        OverlayPanelModule,
        ButtonModule,
        ReactiveFormsModule,
        DotIconButtonModule,
        DotPipesModule
    ],
    exports: [DotLayoutPropertiesComponent],
    providers: []
})
export class DotLayoutPropertiesModule {}
