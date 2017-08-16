import { Component, ViewEncapsulation, OnInit, OnDestroy } from '@angular/core';
import { SiteBrowserState } from '../../../../dotcms-js';

@Component({
    template: './breadcrumb.html',
    encapsulation: ViewEncapsulation.None,

})
export class BreadcrumbDemoShowcase implements OnInit, OnDestroy {

    constructor(private updateService: SiteBrowserState) {}

    ngOnInit() {
        this.updateService.changeFolder(null);
        this.updateService.changeFolder('about-us');
        this.updateService.changeFolder('location');
    }

    ngOnDestroy() {
        this.updateService.changeFolder(null);
    }
}
