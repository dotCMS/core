import { Component, OnInit } from '@angular/core';
import { DotUploadFile } from './models/dot-upload-file.model';

@Component({
    selector: 'dot-site-browser',
    templateUrl: './dot-site-browser.component.html',
    styleUrls: ['./dot-site-browser.component.scss']
})
export class DotSiteBrowserComponent implements OnInit {
    files: DotUploadFile[];

    constructor() {}

    ngOnInit() {}

    getFiles($event: DotUploadFile[]) {
        this.files = $event;
    }
}
