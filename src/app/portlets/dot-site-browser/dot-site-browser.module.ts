import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotSiteBrowserRoutingModule } from './dot-site-browser-routing.module';
import { DotSiteBrowserComponent } from './dot-site-browser.component';

@NgModule({
    imports: [CommonModule, DotSiteBrowserRoutingModule],
    declarations: [DotSiteBrowserComponent]
})
export class DotSiteBrowserModule {}
