import { CommonModule } from '@angular/common';
import { DotcmsConfig } from '../../../api/services/system/dotcms-config';
import { FormsModule } from '@angular/forms';
import { IframeOverlayService } from '../../../api/services/iframe-overlay-service';
import { NgModule } from '@angular/core';
import { SearchableDropDownModule } from '../_common/searchable-dropdown/searchable-dropdown.module';
import { SiteSelectorComponent } from './dot-site-selector.component';
import { SiteService } from '../../../api/services/site-service';

@NgModule({
    declarations: [
        SiteSelectorComponent,
    ],
    exports: [
        SiteSelectorComponent,
        SearchableDropDownModule
    ],
    imports: [
        CommonModule,
        FormsModule,
        SearchableDropDownModule
    ],
    providers: [
        SiteService,
        DotcmsConfig,
        IframeOverlayService,
    ],
})

export class SiteSelectorModule {}