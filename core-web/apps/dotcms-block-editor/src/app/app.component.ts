import { Component } from '@angular/core';

import { EditorComponent } from '@dotcms/new-block-editor';
import { EDITOR_DEMO_CONTENT } from './editor-demo-content';

@Component({
    selector: 'dotcms-root',
    templateUrl: './app.component.html',
    styleUrls: [],
    imports: [EditorComponent],
    standalone: true
})
export class AppComponent {
    protected readonly demoContent = EDITOR_DEMO_CONTENT;
}
