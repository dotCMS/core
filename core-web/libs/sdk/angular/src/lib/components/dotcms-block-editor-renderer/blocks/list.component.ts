import { ChangeDetectionStrategy, Component } from '@angular/core';

@Component({
    selector: 'dotcms-block-editor-renderer-bullet-list',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ul>
            <ng-content />
        </ul>
    `
})
export class DotBulletList {}

@Component({
    selector: 'dotcms-block-editor-renderer-ordered-list',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <ol>
            <ng-content />
        </ol>
    `
})
export class DotOrdererList {}

@Component({
    selector: 'dotcms-block-editor-renderer-list-item',
    changeDetection: ChangeDetectionStrategy.OnPush,
    template: `
        <li>
            <ng-content />
        </li>
    `
})
export class DotListItem {}
