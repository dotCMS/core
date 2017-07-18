import {Component, ViewEncapsulation} from '@angular/core';
import {SiteBrowserState} from "../../../core/util/site-browser.state";

@Component({
    template: require('./breadcrumb.html'),
    encapsulation: ViewEncapsulation.None,

})
export class BreadcrumbDemoShowcase {

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
