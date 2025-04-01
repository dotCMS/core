import { Component } from '@angular/core';

@Component({
    selector: 'dotcms-block-editor-renderer-code-block',
    standalone: true,
    template: `
        <pre>
            <code>
                <ng-content />
            </code>
        </pre>
    `
})
export class DotCMSBlockEditorRendererCodeBlockComponent {}

@Component({
    selector: 'dotcms-block-editor-renderer-block-quote',
    standalone: true,
    template: `
        <blockquote>
            <ng-content />
        </blockquote>
    `
})
export class DotCMSBlockEditorRendererBlockQuoteComponent {}
