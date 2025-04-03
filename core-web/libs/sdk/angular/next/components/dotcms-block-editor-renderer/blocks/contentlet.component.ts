import { AsyncPipe, NgComponentOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, Input } from '@angular/core';

import { ContentNode } from '@dotcms/uve/internal';
import { Contentlet } from '@dotcms/uve/types';

import { DynamicComponentEntity } from '../../../models';
import { CustomRenderer } from '../dotcms-block-editor-renderer.component';

/**
 * Default component for unknown content type
 */
@Component({
    selector: 'dot-default-content',
    template: '<div>Unknown Content Type: {{ contentlet?.contentType }}</div>',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class DotDefaultContentBlock {
    @Input() contentlet: Contentlet<unknown> | undefined;
}

/**
 * DotContent component that renders content based on content type
 */
@Component({
    selector: 'dotcms-block-editor-renderer-contentlet',
    standalone: true,
    imports: [NgComponentOutlet, AsyncPipe],
    changeDetection: ChangeDetectionStrategy.OnPush,
    template:
        '<ng-container *ngComponentOutlet="contentComponent | async; inputs: { contentlet: $data() }"></ng-container>'
})
export class DotContentletBlock {
    @Input() customRenderers: CustomRenderer | undefined;
    @Input() attrs: ContentNode['attrs'];

    contentComponent: DynamicComponentEntity | undefined;

    protected readonly $data = computed(() => this.attrs?.['data'] as Contentlet<unknown>);

    ngOnInit() {
        if (!this.$data()) {
            console.error('DotCMSBlockEditorRendererContentlet: No data provided');
        }

        this.contentComponent =
            this.customRenderers?.[this.$data()?.contentType] ??
            import('./contentlet.component').then((m) => m.DotDefaultContentBlock);
    }
}
