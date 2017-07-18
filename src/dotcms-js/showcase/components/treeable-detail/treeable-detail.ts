import {Component, ViewEncapsulation} from '@angular/core';
import {File} from "../../../core/treeable/shared/file.model";
import {SiteBrowserState} from "../../../core/util/site-browser.state";
import {Treeable} from "../../../core/treeable/shared/treeable.model";

@Component({
    template: require('./treeable-detail.html'),
    encapsulation: ViewEncapsulation.None,

})
export class TreeableDetailComponentDemoShowcase {
    constructor(private updateService: SiteBrowserState) {}

    ngOnInit() {
        let mockImage: any = {
            "inode": "249eeb5c-7002-48e8-9ef3-ea6cd8ea9043",
            "identifier": "4d7daefa-6adb-4b76-896d-c2d9f95b2280",
            "type": "file_asset",
            "modDate": new Date(1354736885805),
            "name": "Hello World",
            "live": true,
            "working": true,
            "archived": false,
            "title": "This is the title",
            "displayType": "File",
            "modUser": "dotcms.org.1",
            "languageId": 1,
            "mimeType": "image/jpeg",
            "fileName": "404.jpg",
            "modUserName": "Admin User",
            "path": "/hello-world",
            "parent": "parent",
        };

        let mockFile: Treeable = Object.assign(new File(), mockImage);
        this.updateService.changeTreeable(mockFile);
    }
}