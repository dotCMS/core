import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DotSiteSelectorFieldComponent } from './dot-site-selector-field.component';
import { DotSiteSelectorModule } from '../dot-site-selector/dot-site-selector.module';

@NgModule({
    declarations: [DotSiteSelectorFieldComponent],
    imports: [CommonModule, DotSiteSelectorModule],
    exports: [DotSiteSelectorFieldComponent],
    providers: []
})
export class SiteSelectorFieldModule {}
