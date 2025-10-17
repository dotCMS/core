import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';

import { DotSiteSelectorFieldComponent } from './dot-site-selector-field.component';

import { DotSiteSelectorComponent } from '../dot-site-selector/dot-site-selector.component';

@NgModule({
    declarations: [DotSiteSelectorFieldComponent],
    imports: [CommonModule, DotSiteSelectorComponent],
    exports: [DotSiteSelectorFieldComponent],
    providers: []
})
export class SiteSelectorFieldModule {}
