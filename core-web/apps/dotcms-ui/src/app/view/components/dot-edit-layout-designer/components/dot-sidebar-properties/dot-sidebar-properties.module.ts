import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotSidebarPropertiesComponent } from './dot-sidebar-properties.component';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { RadioButtonModule } from 'primeng/radiobutton';
import { OverlayPanelModule } from 'primeng/overlaypanel';

@NgModule({
    declarations: [DotSidebarPropertiesComponent],
    imports: [
        ButtonModule,
        CommonModule,
        FormsModule,
        RadioButtonModule,
        OverlayPanelModule,
        ReactiveFormsModule,
        DotPipesModule
    ],
    exports: [DotSidebarPropertiesComponent]
})
export class DotSidebarPropertiesModule {}
