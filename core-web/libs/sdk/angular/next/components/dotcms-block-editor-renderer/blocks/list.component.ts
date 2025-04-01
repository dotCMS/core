import { Component } from '@angular/core';

@Component({
    selector: 'dotcms-block-editor-renderer-bullet-list',
    standalone: true,
    template: `
        <ul>
            <ng-content />
        </ul>
    `
})
export class DotCMSBlockEditorRendererBulletListComponent {}

@Component({
    selector: 'dotcms-block-editor-renderer-ordered-list',
    standalone: true,
    template: `
        <ol>
            <ng-content />
        </ol>
    `
})
export class DotCMSBlockEditorRendererOrderedListComponent {}

@Component({
    selector: 'dotcms-block-editor-renderer-list-item',
    standalone: true,
    template: `
        <li>
            <ng-content />
        </li>
    `
})
export class DotCMSBlockEditorRendererListItemComponent {}
