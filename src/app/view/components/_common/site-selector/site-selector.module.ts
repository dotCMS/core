import { CommonModule } from '@angular/common';
import { DotcmsConfig, SiteService } from 'dotcms-js/dotcms-js';
import { FormsModule } from '@angular/forms';
import { IframeOverlayService } from '../iframe/service/iframe-overlay.service';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../searchable-dropdown/searchable-dropdown.module';
import { SiteSelectorComponent } from './site-selector.component';

@NgModule({
    declarations: [
        SiteSelectorComponent,
    ],
    exports: [
        SiteSelectorComponent
    ],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule
    ]
})

export class SiteSelectorModule {}
