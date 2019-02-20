import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { DotSiteBrowserRoutingModule } from './dot-site-browser-routing.module';
import { DotSiteBrowserComponent } from './dot-site-browser.component';
import { DotDndFilesFoldersDirective } from './directives/dot-dnd-files-folders.directive';

@NgModule({
    imports: [CommonModule, DotSiteBrowserRoutingModule],
    declarations: [DotSiteBrowserComponent, DotDndFilesFoldersDirective]
})
export class DotSiteBrowserModule {}
