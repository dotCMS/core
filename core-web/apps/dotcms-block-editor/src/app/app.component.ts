import { Component } from '@angular/core';

import { EditorComponent } from '@dotcms/new-block-editor';

@Component({
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: [],
    imports: [EditorComponent],
    standalone: true
})
export class AppComponent {}
