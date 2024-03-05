import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { RadioButtonModule } from 'primeng/radiobutton';

import { DotMessagePipe } from '@dotcms/ui';

import { DotSidebarPropertiesComponent } from './dot-sidebar-properties.component';

import { DotPipesModule } from '../../../../pipes/dot-pipes.module';

@NgModule({
    declarations: [DotSidebarPropertiesComponent],
    imports: [
        ButtonModule,
        CommonModule,
        FormsModule,
        RadioButtonModule,
        OverlayPanelModule,
        ReactiveFormsModule,
        DotPipesModule,
        DotMessagePipe
    ],
    exports: [DotSidebarPropertiesComponent]
})
export class DotSidebarPropertiesModule {}
