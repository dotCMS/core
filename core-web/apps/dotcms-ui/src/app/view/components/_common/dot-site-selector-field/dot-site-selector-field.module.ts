import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { DotSiteSelectorModule } from '../dot-site-selector/dot-site-selector.module';
import { DotSiteSelectorFieldComponent } from './dot-site-selector-field.component';

@NgModule({
    declarations: [DotSiteSelectorFieldComponent],
    imports: [CommonModule, DotSiteSelectorModule],
    exports: [DotSiteSelectorFieldComponent],
    providers: []
})
export class SiteSelectorFieldModule {}
