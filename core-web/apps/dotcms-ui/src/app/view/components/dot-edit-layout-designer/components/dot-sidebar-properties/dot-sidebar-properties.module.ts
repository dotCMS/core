import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { DotPipesModule } from '@pipes/dot-pipes.module';
import { ButtonModule } from 'primeng/button';
import { OverlayPanelModule } from 'primeng/overlaypanel';
import { RadioButtonModule } from 'primeng/radiobutton';
import { DotSidebarPropertiesComponent } from './dot-sidebar-properties.component';

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
