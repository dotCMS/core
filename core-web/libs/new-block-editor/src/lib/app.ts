import { ChangeDetectionStrategy, Component } from '@angular/core';

import { DotCMSEditorComponent as EditorComponent } from './editor/editor.component';

@Component({
    selector: 'dot-block-editor-root',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [EditorComponent],
    template: `
        <dot-block-editor />
    `
})
export class App {}
