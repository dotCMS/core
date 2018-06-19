import { DotLayoutSidebarModule } from './dot-layout-property-sidebar/dot-layout-property-sidebar.module';
import { ReactiveFormsModule } from '@angular/forms';
import { OverlayPanelModule, ButtonModule } from 'primeng/primeng';
import { DotLayoutPropertiesItemModule } from './dot-layout-properties-item/dot-layout-properties-item.module';
import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotLayoutPropertiesComponent } from './dot-layout-properties.component';
import { DotThemeSelectorModule } from '../dot-theme-selector/dot-theme-selector.module';

@NgModule({
    declarations: [DotLayoutPropertiesComponent],
    imports: [
        CommonModule,
        DotLayoutPropertiesItemModule,
        DotLayoutSidebarModule,
        OverlayPanelModule,
        ButtonModule,
        ReactiveFormsModule,
        DotThemeSelectorModule
    ],
    exports: [DotLayoutPropertiesComponent],
    providers: []
})
export class DotLayoutPropertiesModule {}
