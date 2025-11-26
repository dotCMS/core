import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-block-editor-renderer-code-block',
    template: `
        <pre>
            <code>
                <ng-content />
            </code>
        </pre>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotCodeBlock {}

@Component({
    selector: 'dotcms-block-editor-renderer-block-quote',
    template: `
        <blockquote>
            <ng-content />
        </blockquote>
    `,
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotBlockQuote {}
